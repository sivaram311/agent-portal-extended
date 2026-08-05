package buzz.delena.agentportal.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SessionEntity::class, MessageEntity::class, PendingPromptEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao
    abstract fun messageDao(): MessageDao
    abstract fun pendingPromptDao(): PendingPromptDao

    companion object {
        private const val DATABASE_NAME = "agent_portal.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME,
                )
                    // Safe for the v1 -> v2 bump only because sessions/messages are a
                    // rebuildable cache of server state. pending_prompts holds text the
                    // user wrote and the server has never seen, so the next schema bump
                    // needs a real Migration rather than another destructive fallback.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
