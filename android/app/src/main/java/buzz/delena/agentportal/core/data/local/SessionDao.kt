package buzz.delena.agentportal.core.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Query("SELECT * FROM sessions ORDER BY updatedAt DESC")
    fun observeSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    fun observeSession(sessionId: String): Flow<SessionEntity?>

    @Upsert
    suspend fun upsert(session: SessionEntity)

    @Upsert
    suspend fun upsertAll(sessions: List<SessionEntity>)
}
