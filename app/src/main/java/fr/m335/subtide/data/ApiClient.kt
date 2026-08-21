package fr.m335.subtide.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.create

/**
 * The one place that turns a configured server URL into a [SubwaveApi]. Kept isolated so a future
 * change to admin auth (see [AdminAuthProvider]) or the API path stays a single-file change, per
 * CLAUDE_CODE_PROMPT.md.
 *
 * Verified against a real SubWave deployment: the domain root serves the Next.js web frontend, and
 * the controller's JSON/image endpoints only answer under an `/api/` prefix — the raw audio mount
 * (`stream.mount` from `/now-playing`) is the one exception, served at the plain root. The user
 * only ever types the plain domain; this prefixing is handled here so nothing else needs it.
 */
object ApiClient {
    private const val API_PATH = "api/"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private fun normalizeRoot(rootUrl: String) = if (rootUrl.endsWith("/")) rootUrl else "$rootUrl/"

    fun create(rootUrl: String): SubwaveApi {
        val apiBaseUrl = normalizeRoot(rootUrl) + API_PATH
        return Retrofit.Builder()
            .baseUrl(apiBaseUrl)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create()
    }

    /** Same API surface, but every request carries admin credentials — only for the admin `dj` calls. */
    fun createAdmin(rootUrl: String, authProvider: AdminAuthProvider): SubwaveApi {
        val apiBaseUrl = normalizeRoot(rootUrl) + API_PATH
        val client = OkHttpClient.Builder()
            .addInterceptor(AdminAuthInterceptor(authProvider))
            .build()
        return Retrofit.Builder()
            .baseUrl(apiBaseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create()
    }

    fun coverUrl(rootUrl: String, subsonicId: String): String =
        "${normalizeRoot(rootUrl)}${API_PATH}cover/$subsonicId"

    /**
     * Resolves a root-relative path like `/stream.mp3` (from `now-playing.stream.mount`) to a full
     * URL. Some private stations gate the actual audio stream behind `?auth=<accessKey>` even
     * though the JSON API stays public — see [ListenerAccess].
     */
    fun rootRelativeUrl(rootUrl: String, path: String, accessKey: String? = null): String {
        val url = normalizeRoot(rootUrl).trimEnd('/') + path
        return if (accessKey.isNullOrBlank()) url else "$url?auth=$accessKey"
    }
}
