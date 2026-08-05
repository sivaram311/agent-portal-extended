package buzz.delena.agentportal.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.workspaceDataStore by preferencesDataStore(name = "workspace_preferences")

class WorkspacePreferences(context: Context) {
    private val dataStore = context.applicationContext.workspaceDataStore

    val recentWorkspaces: Flow<List<String>> = dataStore.data.map { preferences ->
        preferences[RECENT_WORKSPACES]
            ?.let { encoded -> runCatching { Json.decodeFromString<List<String>>(encoded) }.getOrNull() }
            .orEmpty()
    }

    suspend fun recordWorkspace(path: String) {
        val normalized = path.trim()
        if (normalized.isEmpty() || normalized == DEFAULT_WORKSPACE) return
        dataStore.edit { preferences ->
            val current = preferences[RECENT_WORKSPACES]
                ?.let { encoded -> runCatching { Json.decodeFromString<List<String>>(encoded) }.getOrNull() }
                .orEmpty()
            val updated = listOf(normalized) + current.filterNot { it == normalized }
            preferences[RECENT_WORKSPACES] = Json.encodeToString(updated.take(MAX_RECENT_WORKSPACES))
        }
    }

    private companion object {
        val RECENT_WORKSPACES = stringPreferencesKey("recent_workspaces")
        const val MAX_RECENT_WORKSPACES = 8
        const val DEFAULT_WORKSPACE = "demo"
    }
}
