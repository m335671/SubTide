package fr.m335.subtide.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Some private stations gate the actual audio stream behind a `?auth=` query token even though the
 * JSON API stays public — confirmed against a real SubWave deployment, whose `/state` reports
 * `privacy.listenerAuth: true`. Distinct from the admin user/password (`AdminAuthProvider`, for the
 * admin `dj` endpoints): this key is only ever needed to fetch `stream.mount`. Stored encrypted
 * since it's a credential, per CLAUDE_CODE_PROMPT.md.
 *
 * `MasterKey`/`EncryptedSharedPreferences` are flagged deprecated in security-crypto 1.1.0 with no
 * replacement library shipped yet — still the latest stable release in the `androidx.security`
 * group — so the warning is suppressed rather than hand-rolling Tink calls ourselves.
 */
@Suppress("DEPRECATION")
class ListenerAccess(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "subtide_listener_access",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    var accessKey: String?
        get() = prefs.getString(KEY_ACCESS_KEY, null)?.takeIf { it.isNotBlank() }
        set(value) {
            if (value.isNullOrBlank()) {
                prefs.edit().remove(KEY_ACCESS_KEY).apply()
            } else {
                prefs.edit().putString(KEY_ACCESS_KEY, value).apply()
            }
        }

    private companion object {
        const val KEY_ACCESS_KEY = "access_key"
    }
}
