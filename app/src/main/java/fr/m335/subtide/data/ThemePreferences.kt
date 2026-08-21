package fr.m335.subtide.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "subtide_theme")

/**
 * The chosen theme id is local to the listener, per CLAUDE_CODE_PROMPT.md — there's no listener
 * endpoint to persist it server-side, so it stays in DataStore like the server URL.
 */
class ThemePreferences(private val context: Context) {
    private val selectedThemeIdKey = stringPreferencesKey("selected_theme_id")

    val selectedThemeId: Flow<String?> = context.themeDataStore.data.map { it[selectedThemeIdKey] }

    suspend fun setSelectedThemeId(id: String) {
        context.themeDataStore.edit { it[selectedThemeIdKey] = id }
    }
}
