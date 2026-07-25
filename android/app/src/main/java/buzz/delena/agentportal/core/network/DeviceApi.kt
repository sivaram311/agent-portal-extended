package buzz.delena.agentportal.core.network

import buzz.delena.agentportal.core.network.dto.RegisterDeviceTokenRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Device-token registration for push notifications. Same base URL and auth
 * interceptor as AgentPortalApi (built via NetworkModule.provideDeviceApi),
 * kept as a separate Retrofit interface so this file and AgentPortalApi.kt
 * never need simultaneous edits.
 */
interface DeviceApi {

    @POST("api/devices")
    suspend fun registerDevice(@Body request: RegisterDeviceTokenRequest)

    @DELETE("api/devices/{token}")
    suspend fun unregisterDevice(@Path("token") token: String)
}
