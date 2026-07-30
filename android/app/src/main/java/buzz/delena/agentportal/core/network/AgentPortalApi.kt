package buzz.delena.agentportal.core.network

import buzz.delena.agentportal.core.network.dto.AuthConfigDto
import buzz.delena.agentportal.core.network.dto.CreateSessionRequest
import buzz.delena.agentportal.core.network.dto.FileChangeDto
import buzz.delena.agentportal.core.network.dto.MessageDto
import buzz.delena.agentportal.core.network.dto.PathRequest
import buzz.delena.agentportal.core.network.dto.PermissionDecisionRequest
import buzz.delena.agentportal.core.network.dto.PermissionDto
import buzz.delena.agentportal.core.network.dto.PromptRequest
import buzz.delena.agentportal.core.network.dto.SessionDto
import buzz.delena.agentportal.core.network.dto.ToolRunDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit contract for this app's own backend (BuildConfig.API_BASE_URL).
 * Endpoints verified against AuthConfigController and SessionController in
 * the backend source, not guessed from documentation.
 */
interface AgentPortalApi {

    @GET("api/auth/config")
    suspend fun getAuthConfig(): AuthConfigDto

    @GET("api/sessions")
    suspend fun listSessions(): List<SessionDto>

    @POST("api/sessions")
    suspend fun createSession(@Body request: CreateSessionRequest): SessionDto

    @GET("api/sessions/{id}")
    suspend fun getSession(@Path("id") sessionId: String): SessionDto

    @GET("api/sessions/{id}/messages")
    suspend fun getMessages(@Path("id") sessionId: String): List<MessageDto>

    @POST("api/sessions/{id}/prompt")
    suspend fun sendPrompt(
        @Path("id") sessionId: String,
        @Body request: PromptRequest,
    ): MessageDto

    @POST("api/sessions/{id}/cancel")
    suspend fun cancelSession(@Path("id") sessionId: String): Unit

    @GET("api/sessions/{id}/permissions")
    suspend fun getPermissions(@Path("id") sessionId: String): List<PermissionDto>

    @POST("api/sessions/{id}/permissions/{permissionId}")
    suspend fun decidePermission(
        @Path("id") sessionId: String,
        @Path("permissionId") permissionId: String,
        @Body request: PermissionDecisionRequest,
    ): Unit

    @POST("api/sessions/{id}/archive")
    suspend fun archiveSession(@Path("id") sessionId: String): SessionDto

    @POST("api/sessions/{id}/unarchive")
    suspend fun unarchiveSession(@Path("id") sessionId: String): SessionDto

    @GET("api/sessions/{id}/tools")
    suspend fun getTools(@Path("id") sessionId: String): List<ToolRunDto>

    @POST("api/sessions/{id}/subagents/{subId}/abandon")
    suspend fun abandonSubagent(
        @Path("id") sessionId: String,
        @Path("subId") subId: String,
    ): Unit

    @GET("api/sessions/{id}/changes")
    suspend fun getChanges(@Path("id") sessionId: String): List<FileChangeDto>

    @GET("api/sessions/{id}/changes/diff")
    suspend fun getChangeDiff(
        @Path("id") sessionId: String,
        @Query("path") path: String,
    ): FileChangeDto

    @POST("api/sessions/{id}/changes/accept")
    suspend fun acceptChange(
        @Path("id") sessionId: String,
        @Body body: PathRequest,
    ): Unit

    @POST("api/sessions/{id}/changes/reject")
    suspend fun rejectChange(
        @Path("id") sessionId: String,
        @Body body: PathRequest,
    ): Unit

    @GET("api/health")
    suspend fun health(): Response<ResponseBody>
}
