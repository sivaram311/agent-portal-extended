package buzz.delena.agentportal.core.network

import buzz.delena.agentportal.core.data.TokenStore
import java.util.concurrent.CopyOnWriteArrayList
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

    fun connect() {
        connectInternal(allowAuthRetry = true)
    }

    private fun connectInternal(allowAuthRetry: Boolean) {
        if (_connectionState.value == ConnectionState.CONNECTING ||
            _connectionState.value == ConnectionState.CONNECTED
        ) {
            return
        }
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
                    _connectionState.value = ConnectionState.DISCONNECTED
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
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
                        val refreshed = TokenRefresher.tryRefresh(tokenStore)
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
                    _connectionState.value = ConnectionState.FAILED
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
     * connectionState to reach CONNECTED before subscribing; this skeleton
     * does not queue subscriptions sent before the CONNECTED frame arrives.
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

    fun disconnect() {
        webSocket?.send("DISCONNECT\n\n$NUL")
        webSocket?.close(NORMAL_CLOSURE_CODE, "client_disconnect")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    private fun handleIncoming(rawText: String) {
        // Frames may arrive batched/concatenated in one WebSocket text
        // frame; split on the NUL terminator first.
        rawText.split(NUL)
            .map { it.trim('\n', '\r') }
            .filter { it.isNotEmpty() }
            .forEach { rawFrame -> parseFrame(rawFrame)?.let(::dispatch) }
    }

    private fun dispatch(frame: RawStompFrame) {
        when (frame.command) {
            "CONNECTED" -> _connectionState.value = ConnectionState.CONNECTED
            "MESSAGE" -> {
                val destination = frame.headers["destination"] ?: return
                val event = StompEvent(destination, frame.body)
                messageListeners.forEach { it(event) }
            }
            "ERROR" -> _connectionState.value = ConnectionState.FAILED
            else -> Unit
        }
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
    }
}
