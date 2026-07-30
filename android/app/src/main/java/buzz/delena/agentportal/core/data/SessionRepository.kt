package buzz.delena.agentportal.core.data

import buzz.delena.agentportal.core.data.local.MessageDao
import buzz.delena.agentportal.core.data.local.MessageEntity
import buzz.delena.agentportal.core.data.local.SessionDao
import buzz.delena.agentportal.core.data.local.SessionEntity
import buzz.delena.agentportal.core.network.AgentPortalApi
import buzz.delena.agentportal.core.network.dto.CreateSessionRequest
import buzz.delena.agentportal.core.network.dto.FileChangeDto
import buzz.delena.agentportal.core.network.dto.MessageDto
import buzz.delena.agentportal.core.network.dto.PathRequest
import buzz.delena.agentportal.core.network.dto.PermissionDecisionRequest
import buzz.delena.agentportal.core.network.dto.PermissionDto
import buzz.delena.agentportal.core.network.dto.PromptRequest
import buzz.delena.agentportal.core.network.dto.SessionDto
import buzz.delena.agentportal.core.network.dto.ToolRunDto
import kotlinx.coroutines.flow.Flow

/**
 * Session/message data source: Room is the read model for instant
 * cold-start render, the network call is the source of truth pulled in on
 * explicit refresh.
 */
class SessionRepository(
    private val api: AgentPortalApi,
    private val sessionDao: SessionDao,
    private val messageDao: MessageDao,
) {

    fun observeSessions(): Flow<List<SessionEntity>> = sessionDao.observeSessions()

    suspend fun refreshSessions() {
        val sessions = api.listSessions()
        sessionDao.upsertAll(sessions.map { it.toEntity() })
    }

    suspend fun createSession(
        workspacePath: String,
        title: String?,
        provider: String?,
    ): Result<SessionDto> {
        return try {
            val created = api.createSession(
                CreateSessionRequest(
                    title = title,
                    workspacePath = workspacePath,
                    provider = provider,
                    useGuidanceDefaults = null,
                    platformRole = null,
                    platformTaskId = null,
                ),
            )
            sessionDao.upsert(created.toEntity())
            Result.success(created)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    fun observeMessages(sessionId: String): Flow<List<MessageEntity>> =
        messageDao.observeMessages(sessionId)

    suspend fun refreshMessages(sessionId: String) {
        val messages = api.getMessages(sessionId)
        messageDao.upsertAll(messages.map { it.toEntity() })
    }

    suspend fun sendPrompt(sessionId: String, prompt: String): Result<MessageDto> {
        return try {
            val message = api.sendPrompt(sessionId, PromptRequest(prompt))
            messageDao.upsert(message.toEntity())
            Result.success(message)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun getPendingPermissions(sessionId: String): Result<List<PermissionDto>> {
        return try {
            Result.success(api.getPermissions(sessionId))
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun decidePermission(
        sessionId: String,
        permissionId: String,
        decision: String,
        reason: String?,
    ): Result<Unit> {
        return try {
            api.decidePermission(
                sessionId = sessionId,
                permissionId = permissionId,
                request = PermissionDecisionRequest(decision = decision, reason = reason),
            )
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun cancelSession(sessionId: String): Result<Unit> {
        return try {
            api.cancelSession(sessionId)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun archiveSession(sessionId: String): Result<SessionDto> {
        return try {
            val archived = api.archiveSession(sessionId)
            sessionDao.upsert(archived.toEntity())
            Result.success(archived)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun getSession(sessionId: String): Result<SessionDto> {
        return try {
            val session = api.getSession(sessionId)
            sessionDao.upsert(session.toEntity())
            Result.success(session)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun getTools(sessionId: String): Result<List<ToolRunDto>> {
        return try {
            Result.success(api.getTools(sessionId))
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun abandonSubagent(sessionId: String, subagentId: String): Result<Unit> {
        return try {
            api.abandonSubagent(sessionId, subagentId)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun getChanges(sessionId: String): Result<List<FileChangeDto>> {
        return try {
            Result.success(api.getChanges(sessionId))
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun getChangeDiff(sessionId: String, path: String): Result<FileChangeDto> {
        return try {
            Result.success(api.getChangeDiff(sessionId, path))
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun acceptChange(sessionId: String, path: String): Result<Unit> {
        return try {
            api.acceptChange(sessionId, PathRequest(path))
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun rejectChange(sessionId: String, path: String): Result<Unit> {
        return try {
            api.rejectChange(sessionId, PathRequest(path))
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private fun SessionDto.toEntity() = SessionEntity(
        id = id,
        title = title,
        workspacePath = workspacePath,
        status = status.name,
        provider = provider,
        updatedAt = updatedAt,
    )

    private fun MessageDto.toEntity() = MessageEntity(
        id = id,
        sessionId = sessionId,
        role = role.name,
        content = content,
        sequenceNo = sequenceNo,
        createdAt = createdAt,
    )
}
