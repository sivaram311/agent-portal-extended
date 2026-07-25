package buzz.delena.agentportal.core.data

import buzz.delena.agentportal.core.network.DeviceApi
import buzz.delena.agentportal.core.network.dto.RegisterDeviceTokenRequest

/**
 * Thin wrapper around DeviceApi for FCM token lifecycle calls. This is the
 * class a FirebaseMessagingService.onNewToken() implementation calls into --
 * kept separate from DeviceApi itself so the push wiring (currently inert,
 * no Firebase project provisioned yet) has a single small surface to unit
 * test without touching Retrofit directly.
 */
class PushTokenRegistrar(private val deviceApi: DeviceApi) {

    suspend fun register(fcmToken: String): Result<Unit> {
        return try {
            deviceApi.registerDevice(RegisterDeviceTokenRequest(token = fcmToken))
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun unregister(fcmToken: String): Result<Unit> {
        return try {
            deviceApi.unregisterDevice(fcmToken)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
