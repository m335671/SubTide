package fr.m335.subtide.data

import android.content.Context
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Cached values older than this are considered stale and [NowPlayingCache.load] returns null. */
const val NOW_PLAYING_CACHE_MAX_AGE_MS = 60 * 60 * 1000L

data class CachedNowPlaying(
    val nowPlaying: NowPlayingResponse,
    val state: StateResponse,
    val session: SessionResponse,
)

/**
 * Persists the last successfully polled `/now-playing` + `/state` + `/session` payloads so
 * [fr.m335.subtide.ui.player.PlayerViewModel] can redisplay them on the next launch while it
 * re-establishes the server connection, instead of showing "scanning the dial_" every time.
 *
 * Backed by plain [android.content.SharedPreferences] rather than DataStore so [load] can be called
 * synchronously from the ViewModel's constructor and seed its very first UI state — DataStore's
 * Flow-based reads always deliver their first value at least one dispatch later, which showed up as
 * a visible "scanning the dial_" flash before the cached track appeared.
 */
class NowPlayingCache(context: Context) {
    private val prefs = context.getSharedPreferences("subtide_now_playing_cache", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun save(nowPlaying: NowPlayingResponse, state: StateResponse, session: SessionResponse) {
        prefs.edit()
            .putString(KEY_NOW_PLAYING, json.encodeToString(nowPlaying))
            .putString(KEY_STATE, json.encodeToString(state))
            .putString(KEY_SESSION, json.encodeToString(session))
            .putLong(KEY_CACHED_AT, System.currentTimeMillis())
            .apply()
    }

    /** Null if nothing was ever cached, or the cache is older than [NOW_PLAYING_CACHE_MAX_AGE_MS]. */
    fun load(): CachedNowPlaying? {
        val cachedAt = prefs.getLong(KEY_CACHED_AT, -1L).takeIf { it >= 0 } ?: return null
        if (System.currentTimeMillis() - cachedAt > NOW_PLAYING_CACHE_MAX_AGE_MS) return null
        val nowPlaying = prefs.getString(KEY_NOW_PLAYING, null)?.let { json.decodeFromString<NowPlayingResponse>(it) } ?: return null
        val state = prefs.getString(KEY_STATE, null)?.let { json.decodeFromString<StateResponse>(it) } ?: return null
        val session = prefs.getString(KEY_SESSION, null)?.let { json.decodeFromString<SessionResponse>(it) } ?: return null
        return CachedNowPlaying(nowPlaying, state, session)
    }

    private companion object {
        const val KEY_NOW_PLAYING = "now_playing"
        const val KEY_STATE = "state"
        const val KEY_SESSION = "session"
        const val KEY_CACHED_AT = "cached_at"
    }
}
