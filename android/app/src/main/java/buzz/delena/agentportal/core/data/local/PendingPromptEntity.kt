package buzz.delena.agentportal.core.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pending_prompts",
    indices = [Index(value = ["sessionId"])],
)
data class PendingPromptEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val content: String,
    val createdAt: Long,
    val retryCount: Int = 0,
    val status: String = STATUS_PENDING,
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_SENDING = "SENDING"
        const val STATUS_FAILED = "FAILED"
    }
}
