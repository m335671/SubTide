package fr.m335.subtide.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "subtide_settings")

/** Onboarding step 1's persisted result — the configured server base URL, or null if not set yet. */
class ServerPreferences(private val context: Context) {
    private val baseUrlKey = stringPreferencesKey("base_url")

    val baseUrl: Flow<String?> = context.dataStore.data.map { it[baseUrlKey] }

    suspend fun setBaseUrl(url: String) {
        context.dataStore.edit { it[baseUrlKey] = url }
    }

    /** Lets the user return to onboarding step 1 ("Changer de serveur"). */
    suspend fun clearBaseUrl() {
        context.dataStore.edit { it.remove(baseUrlKey) }
    }
}
