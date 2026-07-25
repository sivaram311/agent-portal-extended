package buzz.delena.agentportal.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// Cache for instant cold-start render of the session list, not a full
// mirror of SessionDto -- only the fields needed to draw a list row.
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val title: String?,
    val workspacePath: String,
    val status: String,
    val provider: String?,
    val updatedAt: String,
)
