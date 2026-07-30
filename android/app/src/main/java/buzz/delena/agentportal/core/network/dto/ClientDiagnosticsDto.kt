package buzz.delena.agentportal.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ClientDiagnosticsRequest(
    val deviceId: String,
    val appVersion: String? = null,
    val versionCode: Int? = null,
    val platform: String = "android",
    val reason: String = "manual",
    val createdAt: String? = null,
    val lines: String,
)

@Serializable
data class ClientDiagnosticsResponse(
    val ok: Boolean? = null,
    val path: String? = null,
    val bytes: Long? = null,
)
