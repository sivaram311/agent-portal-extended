package buzz.delena.agentportal.core.diagnostics

import android.content.Context
import android.os.Build
import android.provider.Settings
import buzz.delena.agentportal.BuildConfig
import buzz.delena.agentportal.core.network.AgentPortalApi
import buzz.delena.agentportal.core.network.dto.ClientDiagnosticsRequest
import buzz.delena.agentportal.core.network.userFacingErrorMessage
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DiagnosticsRepository(
    private val appContext: Context,
    private val api: AgentPortalApi,
) {
    private val pendingCrashFile: File
        get() = File(appContext.filesDir, PENDING_CRASH_FILE)

    fun deviceId(): String {
        val androidId = runCatching {
            Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull()
        return androidId?.takeIf { it.isNotBlank() } ?: "unknown-device"
    }

    fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { persistCrash(thread, throwable) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun persistCrash(thread: Thread, throwable: Throwable) {
        val body = buildString {
            appendLine("=== UNCAUGHT EXCEPTION ===")
            appendLine("thread=${thread.name}")
            appendLine("at=${Instant.now()}")
            appendLine("version=${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("device=${Build.MANUFACTURER} ${Build.MODEL} sdk=${Build.VERSION.SDK_INT}")
            appendLine()
            appendLine(throwable.stackTraceToString())
            appendLine()
            appendLine("=== RING BUFFER ===")
            appendLine(DiagnosticLogBuffer.snapshot())
        }
        pendingCrashFile.writeText(body)
        AppLog.e(TAG, "Persisted crash dump to ${pendingCrashFile.name}", throwable)
    }

    suspend fun uploadPendingCrashIfAny(): Result<Unit> = withContext(Dispatchers.IO) {
        val file = pendingCrashFile
        if (!file.exists()) return@withContext Result.success(Unit)
        val content = runCatching { file.readText() }.getOrElse {
            return@withContext Result.failure(it)
        }
        upload(reason = "crash", extra = content)
            .onSuccess {
                runCatching { file.delete() }
                AppLog.i(TAG, "Uploaded pending crash dump")
            }
            .onFailure { t ->
                AppLog.w(TAG, "Pending crash upload failed; keeping file for retry", t)
            }
            .map { }
    }

    suspend fun sendManualDiagnostics(): Result<String> = withContext(Dispatchers.IO) {
        upload(reason = "manual", extra = null).map { it }
    }

    private suspend fun upload(reason: String, extra: String?): Result<String> {
        return try {
            val buffer = DiagnosticLogBuffer.snapshot()
            val logcat = captureOwnLogcat()
            val body = buildString {
                appendLine("=== APP BUFFER ===")
                appendLine(buffer.ifBlank { "(empty)" })
                if (!extra.isNullOrBlank()) {
                    appendLine()
                    appendLine(extra)
                }
                if (logcat.isNotBlank()) {
                    appendLine()
                    appendLine("=== LOGCAT (pid) ===")
                    appendLine(logcat)
                }
            }.take(MAX_UPLOAD_CHARS)

            AppLog.i(TAG, "Uploading diagnostics reason=$reason chars=${body.length}")
            val response = api.uploadClientDiagnostics(
                ClientDiagnosticsRequest(
                    deviceId = deviceId(),
                    appVersion = BuildConfig.VERSION_NAME,
                    versionCode = BuildConfig.VERSION_CODE,
                    platform = "android",
                    reason = reason,
                    createdAt = Instant.now().toString(),
                    lines = body,
                ),
            )
            Result.success(response.path ?: "ok")
        } catch (t: Throwable) {
            AppLog.e(TAG, "Diagnostics upload failed", t)
            Result.failure(Exception(userFacingErrorMessage(t), t))
        }
    }

    private fun captureOwnLogcat(): String {
        return runCatching {
            val pid = android.os.Process.myPid().toString()
            val process = ProcessBuilder("logcat", "-d", "-t", "300", "--pid=$pid")
                .redirectErrorStream(true)
                .start()
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.readText().take(80_000)
            }
        }.getOrDefault("")
    }

    private companion object {
        const val TAG = "Diagnostics"
        const val PENDING_CRASH_FILE = "pending-crash.log"
        const val MAX_UPLOAD_CHARS = 900_000
    }
}
