package buzz.delena.agentportal.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import buzz.delena.agentportal.core.data.TokenStore
import java.net.ConnectException
import java.net.SocketException
import java.net.UnknownHostException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, FAILED }

/** A parsed STOMP MESSAGE frame, handed to callers as-is; per-event-type
 * parsing of bodyJson is left to the caller (a future ChatViewModel). */
data class StompEvent(val destination: String, val bodyJson: String)

private data class RawStompFrame(
    val command: String,
    val headers: Map<String, String>,
    val body: String,
)

/**
 * Minimal hand-rolled STOMP-over-WebSocket client -- no third-party STOMP
 * library dependency (they are largely unmaintained); STOMP 1.2 is a simple
 * newline-delimited text protocol and only the CONNECT / CONNECTED /
 * SUBSCRIBE / MESSAGE / ERROR subset is needed here.
 *
 * Connects to "{wsBaseUrl}/ws/websocket", not "/ws" -- "/ws" is the
 * SockJS-registered endpoint (backend uses withSockJS()), but SockJS's own
 * native-websocket transport is reachable at the "/websocket" suffix, which
 * lets a plain WebSocket client skip the SockJS JS library entirely. This is
 * a deliberate, verified design choice.
 *
 * Auth: the access token is appended as an "access_token" query parameter
 * on the connect URL, matching the backend's documented pattern for its
 * SockJS-registered endpoints.
 *
 * Keeps the socket alive with STOMP heartbeats (client → server newlines)
 * plus OkHttp WebSocket pings, and auto-reconnects while [connect] has been
 * requested and [disconnect] has not.
 *
 * Reconnect is connectivity-aware: [ConnectivityManager.NetworkCallback]
 * pauses retries while the default network is down (wifi↔cellular handoff,
 * Doze, airplane mode) so we do not hammer DNS during the transition window.
 * When a network is usable again, reconnect resumes immediately (backoff
 * reset). Failures that are not "no network" use exponential backoff
 * (~1s … ~30s). Transient DNS / socket blips stay on the retry path and do
 * not surface as [ConnectionState.FAILED].
 */
class StompWebSocketClient(
    private val okHttpClient: OkHttpClient,
    private val wsBaseUrl: String,
    private val tokenStore: TokenStore,
) {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var webSocket: WebSocket? = null
    private val subscriptionCounter = AtomicInteger(0)
    private val messageListeners = CopyOnWriteArrayList<(StompEvent) -> Unit>()
    private val wantConnected = AtomicBoolean(false)
    private val connectRefs = AtomicInteger(0)
    private val reconnectAttempt = AtomicInteger(0)
    private val hasUsableNetwork = AtomicBoolean(true)
    private val networkCallbackRegistered = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val scheduler: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "stomp-heartbeat").apply { isDaemon = true }
        }
    private var heartbeatFuture: ScheduledFuture<*>? = null
    private var reconnectFuture: ScheduledFuture<*>? = null

    fun connect() {
        wantConnected.set(true)
        ensureNetworkMonitoring()
        connectInternal(allowAuthRetry = true)
    }

    /** Ref-count so leaving Chat does not kill a socket Sessions still needs. */
    fun acquire() {
        if (connectRefs.incrementAndGet() == 1) {
            connect()
        } else if (_connectionState.value != ConnectionState.CONNECTED &&
            _connectionState.value != ConnectionState.CONNECTING
        ) {
            connect()
        }
    }

    fun release() {
        if (connectRefs.decrementAndGet() <= 0) {
            connectRefs.set(0)
            disconnect()
        }
    }

    private fun connectInternal(allowAuthRetry: Boolean) {
        if (!wantConnected.get()) return
        if (_connectionState.value == ConnectionState.CONNECTING ||
            _connectionState.value == ConnectionState.CONNECTED
        ) {
            return
        }
        if (!hasUsableNetwork.get()) {
            // Interface is down / transitioning — wait for NetworkCallback.
            _connectionState.value = ConnectionState.DISCONNECTED
            cancelReconnect()
            return
        }
        cancelReconnect()
        _connectionState.value = ConnectionState.CONNECTING

        val request = buildConnectRequest()
        webSocket = okHttpClient.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    webSocket.send(CONNECT_FRAME)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleIncoming(text)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    stopHeartbeat()
                    _connectionState.value = ConnectionState.DISCONNECTED
                    scheduleReconnectIfWanted()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    stopHeartbeat()
                    // This backend returns 403 (never 401) for an expired or
                    // missing access token, including on the WS upgrade
                    // request itself (confirmed directly against the live
                    // server: an unauthenticated /ws/websocket handshake
                    // gets 403, same as /api/**). A stale token at connect()
                    // time -- e.g. the ~15min access-token TTL expiring while
                    // a chat screen sits open -- used to fail the handshake
                    // exactly like this with no automatic retry, which is
                    // what made realtime updates silently stop until the
                    // screen was closed and reopened (a fresh ViewModel
                    // reads a by-then-refreshed REST token and reconnects).
                    // One refresh-and-reconnect attempt here closes that gap
                    // without the caller needing to know it happened.
                    if (allowAuthRetry && response?.code == 403 && tokenStore.getRefreshToken() != null) {
                        // Do not wipe a still-valid access JWT on a network blip
                        // during refresh — same policy as TokenAuthenticator.
                        val refreshed = TokenRefresher.tryRefresh(tokenStore, clearOnFailure = false)
                        if (refreshed) {
                            // connectInternal()'s own guard at the top only
                            // proceeds when state is DISCONNECTED/FAILED --
                            // it's still CONNECTING right now (nothing else
                            // has touched it since this handshake started),
                            // so without resetting it first the recursive
                            // call below would hit that guard and silently
                            // no-op, leaving the state stuck at CONNECTING
                            // forever instead of ever reaching CONNECTED or
                            // FAILED. (Caught in review before this shipped:
                            // the first version of this retry had exactly
                            // that bug.)
                            _connectionState.value = ConnectionState.DISCONNECTED
                            connectInternal(allowAuthRetry = false)
                            return
                        }
                    }
                    // Transient DNS / socket aborts (wifi↔cellular, brief
                    // offline windows) are recoverable — stay on the backoff
                    // path as DISCONNECTED so the UI does not show a hard
                    // "Realtime offline" while the next attempt may succeed.
                    val transient = isTransientNetworkFailure(t)
                    _connectionState.value = if (transient) {
                        ConnectionState.DISCONNECTED
                    } else {
                        ConnectionState.FAILED
                    }
                    scheduleReconnectIfWanted()
                    buzz.delena.agentportal.core.diagnostics.AppLog.w(
                        "StompWS",
                        "WebSocket failure code=${response?.code} ${t.message}",
                        t,
                    )
                }
            },
        )
    }

    private fun buildConnectRequest(): Request {
        val base = wsBaseUrl.removeSuffix("/")
        val accessToken = tokenStore.getAccessToken()
        val url = buildString {
            append(base)
            append("/ws/websocket")
            if (!accessToken.isNullOrBlank()) {
                append("?access_token=")
                append(accessToken)
            }
        }
        return Request.Builder().url(url).build()
    }

    /**
     * Subscribes to "/topic/sessions/{sessionId}" and emits every MESSAGE
     * frame received for it. Callers should generally wait for
     * connectionState to reach CONNECTED before collecting; use
     * flatMapLatest on connectionState so a drop cancels this flow and a
     * later CONNECTED re-subscribes.
     */
    fun subscribeToSession(sessionId: String): Flow<StompEvent> = callbackFlow {
        val destination = "/topic/sessions/$sessionId"
        val subscriptionId = "sub-${subscriptionCounter.incrementAndGet()}"

        val listener: (StompEvent) -> Unit = { event ->
            if (event.destination == destination) {
                trySend(event)
            }
        }
        messageListeners.add(listener)

        val frame = "SUBSCRIBE\nid:$subscriptionId\ndestination:$destination\n\n$NUL"
        webSocket?.send(frame)

        awaitClose { messageListeners.remove(listener) }
    }

    fun forceReconnect() {
        wantConnected.set(true)
        ensureNetworkMonitoring()
        cancelReconnect()
        stopHeartbeat()
        reconnectAttempt.set(0)
        runCatching { webSocket?.cancel() }
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
        connectInternal(allowAuthRetry = true)
    }

    fun disconnect() {
        connectRefs.set(0)
        wantConnected.set(false)
        cancelReconnect()
        stopHeartbeat()
        stopNetworkMonitoring()
        reconnectAttempt.set(0)
        runCatching { webSocket?.send("DISCONNECT\n\n$NUL") }
        webSocket?.close(NORMAL_CLOSURE_CODE, "client_disconnect")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    /**
     * Schedules a reconnect with exponential backoff (1s, 2s, 4s, … capped
     * at [MAX_RECONNECT_DELAY_SEC]), unless there is no usable network — in
     * that case the attempt is deferred until [ConnectivityManager] reports
     * the default network is back.
     */
    private fun scheduleReconnectIfWanted(immediate: Boolean = false) {
        if (!wantConnected.get()) return
        cancelReconnect()
        if (!hasUsableNetwork.get()) {
            buzz.delena.agentportal.core.diagnostics.AppLog.d(
                "StompWS",
                "Reconnect deferred until network is available",
            )
            return
        }
        val delaySec = if (immediate) {
            0L
        } else {
            val attempt = reconnectAttempt.incrementAndGet().coerceAtMost(MAX_RECONNECT_ATTEMPT_EXPONENT)
            (1L shl (attempt - 1)).coerceAtMost(MAX_RECONNECT_DELAY_SEC)
        }
        reconnectFuture = scheduler.schedule({
            if (!wantConnected.get()) return@schedule
            if (!hasUsableNetwork.get()) {
                // Dropped during the backoff wait — NetworkCallback will resume.
                return@schedule
            }
            if (_connectionState.value == ConnectionState.CONNECTED ||
                _connectionState.value == ConnectionState.CONNECTING
            ) {
                return@schedule
            }
            _connectionState.value = ConnectionState.DISCONNECTED
            connectInternal(allowAuthRetry = true)
        }, delaySec, TimeUnit.SECONDS)
    }

    private fun cancelReconnect() {
        reconnectFuture?.cancel(false)
        reconnectFuture = null
    }

    private fun ensureNetworkMonitoring() {
        if (!networkCallbackRegistered.compareAndSet(false, true)) return
        val cm = connectivityManager()
        if (cm == null) {
            networkCallbackRegistered.set(false)
            hasUsableNetwork.set(true)
            return
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                onNetworkUsabilityChanged(evaluateNetwork(cm, network))
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                onNetworkUsabilityChanged(isUsable(networkCapabilities))
            }

            override fun onLost(network: Network) {
                onNetworkUsabilityChanged(false)
            }
        }
        try {
            // Seed from the current default network before registering so the
            // first connect() does not race an empty callback window.
            hasUsableNetwork.set(evaluateDefaultNetwork(cm))
            cm.registerDefaultNetworkCallback(callback, mainHandler)
            networkCallback = callback
        } catch (e: SecurityException) {
            networkCallbackRegistered.set(false)
            networkCallback = null
            hasUsableNetwork.set(true)
            buzz.delena.agentportal.core.diagnostics.AppLog.w(
                "StompWS",
                "NetworkCallback unavailable; reconnect will use backoff only",
                e,
            )
        } catch (e: RuntimeException) {
            networkCallbackRegistered.set(false)
            networkCallback = null
            hasUsableNetwork.set(true)
            buzz.delena.agentportal.core.diagnostics.AppLog.w(
                "StompWS",
                "NetworkCallback registration failed; reconnect will use backoff only",
                e,
            )
        }
    }

    private fun stopNetworkMonitoring() {
        val callback = networkCallback ?: run {
            networkCallbackRegistered.set(false)
            return
        }
        networkCallback = null
        networkCallbackRegistered.set(false)
        val cm = connectivityManager()
        if (cm != null) {
            runCatching { cm.unregisterNetworkCallback(callback) }
        }
    }

    private fun onNetworkUsabilityChanged(usable: Boolean) {
        val wasUsable = hasUsableNetwork.getAndSet(usable)
        if (!wantConnected.get()) return
        if (!usable) {
            // Stop blind retries into a dead interface (DNS noise).
            cancelReconnect()
            return
        }
        if (!wasUsable) {
            // Network restored after a gap — resume fast, do not keep the
            // outage-era backoff exponent.
            reconnectAttempt.set(0)
            if (_connectionState.value != ConnectionState.CONNECTED &&
                _connectionState.value != ConnectionState.CONNECTING
            ) {
                scheduleReconnectIfWanted(immediate = true)
            }
        }
    }

    private fun connectivityManager(): ConnectivityManager? {
        val ctx = resolveApplicationContext() ?: return null
        return ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    }

    /**
     * AppContainer does not pass a Context into this client (and parallel
     * work owns that wiring). Resolve the process Application, which is
     * available by the time [connect] / [acquire] runs.
     */
    private fun resolveApplicationContext(): Context? {
        return try {
            val activityThread = Class.forName("android.app.ActivityThread")
            val app = activityThread.getMethod("currentApplication").invoke(null)
            (app as? Context)?.applicationContext
        } catch (_: Throwable) {
            null
        }
    }

    private fun evaluateDefaultNetwork(cm: ConnectivityManager): Boolean {
        return try {
            val network = cm.activeNetwork ?: return false
            evaluateNetwork(cm, network)
        } catch (_: SecurityException) {
            true
        }
    }

    private fun evaluateNetwork(cm: ConnectivityManager, network: Network): Boolean {
        return try {
            isUsable(cm.getNetworkCapabilities(network))
        } catch (_: SecurityException) {
            true
        }
    }

    private fun isUsable(caps: NetworkCapabilities?): Boolean {
        if (caps == null) return false
        // INTERNET + VALIDATED ≈ real outbound path (DNS/captive portal
        // cleared). Waiting for VALIDATED avoids reconnect storms during the
        // brief unvalidated window of a wifi↔cellular handoff.
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun startHeartbeat(clientSendMs: Long) {
        stopHeartbeat()
        if (clientSendMs <= 0L) return
        val interval = clientSendMs.coerceAtLeast(5_000L)
        heartbeatFuture = scheduler.scheduleAtFixedRate({
            val socket = webSocket ?: return@scheduleAtFixedRate
            if (_connectionState.value != ConnectionState.CONNECTED) return@scheduleAtFixedRate
            // STOMP heartbeat: a single LF (or NUL-terminated empty frame).
            socket.send("\n")
        }, interval, interval, TimeUnit.MILLISECONDS)
    }

    private fun stopHeartbeat() {
        heartbeatFuture?.cancel(false)
        heartbeatFuture = null
    }

    private fun handleIncoming(rawText: String) {
        // Frames may arrive batched/concatenated in one WebSocket text
        // frame; split on the NUL terminator first. Pure heartbeat LFs
        // produce empty pieces after split/trim and are ignored.
        rawText.split(NUL)
            .map { it.trim('\n', '\r') }
            .filter { it.isNotEmpty() }
            .forEach { rawFrame -> parseFrame(rawFrame)?.let(::dispatch) }
    }

    private fun dispatch(frame: RawStompFrame) {
        when (frame.command) {
            "CONNECTED" -> {
                reconnectAttempt.set(0)
                _connectionState.value = ConnectionState.CONNECTED
                val heartBeat = frame.headers["heart-beat"] ?: frame.headers["heartbeat"]
                val clientSendMs = parseClientSendHeartbeatMs(heartBeat)
                startHeartbeat(clientSendMs)
            }
            "MESSAGE" -> {
                val destination = frame.headers["destination"] ?: return
                val event = StompEvent(destination, frame.body)
                messageListeners.forEach { it(event) }
            }
            "ERROR" -> {
                stopHeartbeat()
                _connectionState.value = ConnectionState.FAILED
                scheduleReconnectIfWanted()
            }
            else -> Unit
        }
    }

    /** STOMP heart-beat header is "cx,cy" — client send interval, server send interval (ms). */
    private fun parseClientSendHeartbeatMs(header: String?): Long {
        if (header.isNullOrBlank()) return DEFAULT_CLIENT_HEARTBEAT_MS
        val clientPart = header.split(',').firstOrNull()?.trim()?.toLongOrNull() ?: return DEFAULT_CLIENT_HEARTBEAT_MS
        return if (clientPart <= 0L) DEFAULT_CLIENT_HEARTBEAT_MS else clientPart
    }

    private fun parseFrame(rawFrame: String): RawStompFrame? {
        val lines = rawFrame.split("\n")
        val command = lines.firstOrNull()?.trim().orEmpty()
        if (command.isEmpty()) return null

        val headers = mutableMapOf<String, String>()
        var bodyStartIndex = lines.size
        for (i in 1 until lines.size) {
            val line = lines[i]
            if (line.isEmpty()) {
                bodyStartIndex = i + 1
                break
            }
            val separatorIndex = line.indexOf(':')
            if (separatorIndex > 0) {
                headers[line.substring(0, separatorIndex)] = line.substring(separatorIndex + 1)
            }
        }
        val body = if (bodyStartIndex < lines.size) {
            lines.subList(bodyStartIndex, lines.size).joinToString("\n")
        } else {
            ""
        }
        return RawStompFrame(command, headers, body)
    }

    private companion object {
        // The STOMP frame terminator is the NUL character. Derived from the
        // Kotlin stdlib constant (Char.MIN_VALUE) rather than a unicode
        // escape literal in source, to sidestep any tooling that mishandles
        // a literal control-character escape sequence inside a string.
        val NUL: Char = Char.MIN_VALUE
        val CONNECT_FRAME = "CONNECT\naccept-version:1.2\nheart-beat:10000,10000\n\n$NUL"
        const val NORMAL_CLOSURE_CODE = 1000
        const val DEFAULT_CLIENT_HEARTBEAT_MS = 10_000L
        /** Cap of the attempt exponent so delay stays at [MAX_RECONNECT_DELAY_SEC]. */
        const val MAX_RECONNECT_ATTEMPT_EXPONENT = 6
        const val MAX_RECONNECT_DELAY_SEC = 30L

        fun isTransientNetworkFailure(t: Throwable): Boolean {
            if (t is UnknownHostException || t is ConnectException || t is SocketException) {
                return true
            }
            var cur: Throwable? = t
            while (cur != null) {
                if (cur is UnknownHostException) return true
                val msg = cur.message.orEmpty()
                if (msg.contains("Unable to resolve host", ignoreCase = true) ||
                    msg.contains("Software caused connection abort", ignoreCase = true) ||
                    msg.contains("Socket closed", ignoreCase = true) ||
                    msg.contains("Connection reset", ignoreCase = true) ||
                    msg.contains("Network is unreachable", ignoreCase = true) ||
                    msg.contains("failed to connect", ignoreCase = true)
                ) {
                    return true
                }
                cur = cur.cause
            }
            return false
        }
    }
}
