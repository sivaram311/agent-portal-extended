package buzz.delena.agentportal.core.network.dto

import kotlinx.serialization.Serializable

// Matches the backend's new RegisterDeviceTokenRequest record (com.agentportal.dto),
// POSTed to /api/devices to register this install for push notifications.
@Serializable
data class RegisterDeviceTokenRequest(
    val token: String,
    val platform: String = "android",
)
