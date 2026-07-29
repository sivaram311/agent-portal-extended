package buzz.delena.agentportal.core.network

import org.json.JSONObject
import retrofit2.HttpException

/**
 * Turns Retrofit/OkHttp failures into short user-facing copy. Prefer the
 * backend's `error` JSON field (e.g. "Session already has an active run")
 * over a bare status code.
 */
fun userFacingErrorMessage(t: Throwable): String {
    val http = t as? HttpException
    if (http != null) {
        val bodyMessage = http.errorBodyMessage()
        return when (http.code()) {
            401, 403 -> bodyMessage
                ?: "Your session expired and couldn't be refreshed — please sign in again."
            400 -> bodyMessage
                ?: "Request rejected. If a run is already active, wait for it to finish or cancel it."
            408, 499 -> bodyMessage
                ?: "The server took too long to start the agent. Check the session — it may still be running."
            in 500..599 -> bodyMessage ?: "Server error (${http.code()}). Please try again."
            else -> bodyMessage ?: "Server error (${http.code()}). Please try again."
        }
    }
    val message = t.message?.takeIf { it.isNotBlank() }.orEmpty()
    return when {
        message.contains("timeout", ignoreCase = true) ||
            message.contains("timed out", ignoreCase = true) ->
            "Timed out waiting for the portal. The agent may still be starting — pull to refresh."
        message.contains("Unable to resolve host", ignoreCase = true) ||
            message.contains("failed to connect", ignoreCase = true) ->
            "Couldn't reach the server. Check your connection and try again."
        message.isNotBlank() -> message
        else -> "Couldn't reach the server. Check your connection and try again."
    }
}

private fun HttpException.errorBodyMessage(): String? {
    val raw = runCatching { response()?.errorBody()?.string() }.getOrNull()
        ?.takeIf { it.isNotBlank() }
        ?: return null
    return runCatching {
        val json = JSONObject(raw)
        sequenceOf("error", "message", "detail")
            .mapNotNull { key -> json.optString(key).takeIf { it.isNotBlank() } }
            .firstOrNull()
    }.getOrNull() ?: raw.take(180)
}
