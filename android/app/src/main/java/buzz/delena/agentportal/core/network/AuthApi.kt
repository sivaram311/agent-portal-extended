package buzz.delena.agentportal.core.network

import buzz.delena.agentportal.core.network.dto.LoginRequest
import buzz.delena.agentportal.core.network.dto.LoginResponseDto
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * Contract for the CSS auth server, which is a different host than this
 * app's own backend and whose base URL (authConfig.authUrl) is only known at
 * runtime after GET api/auth/config resolves. Every call therefore supplies
 * a fully-qualified @Url instead of a relative path, and the Retrofit
 * instance this interface is attached to carries a placeholder base URL that
 * is never actually used (Retrofit requires one to be set regardless).
 */
interface AuthApi {

    @POST
    suspend fun login(
        @Url url: String,
        @Body request: LoginRequest,
    ): LoginResponseDto
}
