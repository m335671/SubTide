package fr.m335.subtide.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Admin username/password for `/dj/skip`, entered once from the admin panel and stored encrypted —
 * never in plain DataStore — per CLAUDE_CODE_PROMPT.md. Distinct from [ListenerAccess]'s stream key.
 *
 * `MasterKey`/`EncryptedSharedPreferences` are flagged deprecated in security-crypto 1.1.0 with no
 * replacement library shipped yet — still the latest stable release in the `androidx.security`
 * group — so the warning is suppressed rather than hand-rolling Tink calls ourselves.
 */
@Suppress("DEPRECATION")
class AdminCredentials(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "subtide_admin_credentials",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    val username: String? get() = prefs.getString(KEY_USERNAME, null)
    val password: String? get() = prefs.getString(KEY_PASSWORD, null)
    val hasCredentials: Boolean get() = username != null && password != null

    /** Reactive mirror of [hasCredentials] — lets the pager add/remove the Skip tab (feature 9). */
    private val _hasCredentials = MutableStateFlow(hasCredentials)
    val hasCredentialsFlow: StateFlow<Boolean> = _hasCredentials.asStateFlow()

    fun save(username: String, password: String) {
        prefs.edit().putString(KEY_USERNAME, username).putString(KEY_PASSWORD, password).apply()
        _hasCredentials.value = true
    }

    fun clear() {
        prefs.edit().remove(KEY_USERNAME).remove(KEY_PASSWORD).apply()
        _hasCredentials.value = false
    }

    private companion object {
        const val KEY_USERNAME = "username"
        const val KEY_PASSWORD = "password"
    }
}
