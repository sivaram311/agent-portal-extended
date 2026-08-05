package buzz.delena.agentportal.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingPromptDao {

    @Query("SELECT * FROM pending_prompts WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    fun observePending(sessionId: String): Flow<List<PendingPromptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(prompt: PendingPromptEntity)

    @Query("DELETE FROM pending_prompts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM pending_prompts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PendingPromptEntity?

    @Query("UPDATE pending_prompts SET status = :status, retryCount = :retryCount WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, retryCount: Int)

    @Query("SELECT * FROM pending_prompts ORDER BY createdAt ASC")
    suspend fun getAllPending(): List<PendingPromptEntity>
}
