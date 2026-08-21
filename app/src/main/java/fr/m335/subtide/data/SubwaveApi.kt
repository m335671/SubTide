package fr.m335.subtide.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Verified against a real SUB/WAVE deployment on 2026-08-19 — these are the actual field
 * names, not guesses. `/now-playing` wraps the track under `nowPlaying`, the on-air DJ is a nested
 * `dj` object (not a flat `djOnAir` string), listener count is `{current, peak}`, and the key field
 * is `musicalKey`, not `key`. Every field stays nullable and unknown keys are ignored, so a future
 * server that differs slightly still degrades to a missing value instead of crashing.
 */
@Serializable
data class NowPlayingTrack(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    @SerialName("subsonic_id") val subsonicId: String? = null,
    val genre: String? = null,
    val bpm: Int? = null,
    val musicalKey: String? = null,
    val moods: List<String>? = null,
    val year: Int? = null,
    /** Track length in seconds. */
    val duration: Int? = null,
    /** Unix seconds the track started playing — combine with [duration] for an elapsed/total display. */
    val timestamp: Long? = null,
)

@Serializable
data class NowPlayingDj(
    val name: String? = null,
    val tagline: String? = null,
    val avatar: String? = null,
    val station: String? = null,
)

@Serializable
data class NowPlayingListeners(
    val current: Int? = null,
    val peak: Int? = null,
)

@Serializable
data class StreamInfo(
    val mount: String? = null,
    val format: String? = null,
    val bitrate: Int? = null,
    val opusEnabled: Boolean? = null,
    val flacEnabled: Boolean? = null,
    val aacEnabled: Boolean? = null,
)

@Serializable
data class NowPlayingResponse(
    val nowPlaying: NowPlayingTrack? = null,
    val dj: NowPlayingDj? = null,
    val listeners: NowPlayingListeners? = null,
    val streamOnline: Boolean? = null,
    val stream: StreamInfo? = null,
    val llmTokens: Int? = null,
)

@Serializable
data class HistoryTrack(
    @SerialName("subsonic_id") val subsonicId: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    /** ISO-8601 instants (e.g. `2026-08-18T21:22:43.589Z`). */
    val startedAt: String? = null,
    val endedAt: String? = null,
)

@Serializable
data class StateTheme(
    val active: String? = null,
)

@Serializable
data class StationInfo(
    val id: String? = null,
    val name: String? = null,
)

@Serializable
data class StateResponse(
    val current: HistoryTrack? = null,
    val upcoming: List<HistoryTrack>? = null,
    /** Most-recent-first, per the real deployment's ordering. */
    val history: List<HistoryTrack>? = null,
    val theme: StateTheme? = null,
    val station: StationInfo? = null,
    /** IANA zone (e.g. `Europe/Paris`) — timestamps display in this zone, not the device's. */
    val timezone: String? = null,
)

/**
 * A turn in the Booth transcript. `role` seen in practice: `segment` (an actually-aired spoken
 * line — carries `meta.airedAt`), `dj` (internal reasoning about a track pick, never spoken aloud),
 * `track` (a play announcement), `event` (station-internal narration). `kind == "system"` turns
 * must never be shown, per CLAUDE_CODE_PROMPT.md. `t` is an ISO-8601 instant, not epoch millis.
 */
@Serializable
data class BoothMessage(
    val t: String? = null,
    val role: String? = null,
    val kind: String? = null,
    val text: String? = null,
)

@Serializable
data class SessionResponse(
    /** Oldest-first — reverse for most-recent-first display. */
    val messages: List<BoothMessage>? = null,
)

/**
 * The 7 themable tokens as raw CSS strings (hex, `rgb()`, `oklch()`, or `color-mix()`) — see
 * [fr.m335.subtide.data.parseCssColor] for what's actually parsed, and `ThemeMapper.kt` for how a
 * [ThemeDto] becomes a [fr.m335.subtide.ui.theme.SubwaveThemeOption].
 */
@Serializable
data class ThemeTokensDto(
    @SerialName("--bg") val bg: String? = null,
    @SerialName("--ink") val ink: String? = null,
    @SerialName("--muted") val muted: String? = null,
    @SerialName("--accent") val accent: String? = null,
    @SerialName("--overlay") val overlay: String? = null,
    @SerialName("--soft-border") val softBorder: String? = null,
    @SerialName("--field") val field: String? = null,
)

@Serializable
data class ThemeDto(
    val id: String? = null,
    val name: String? = null,
    val description: String? = null,
    val mode: String? = null,
    val tokens: ThemeTokensDto? = null,
)

@Serializable
data class ThemesResponse(
    val active: String? = null,
    val themes: List<ThemeDto>? = null,
)

@Serializable
data class RequestSubmitBody(
    val text: String,
    val name: String? = null,
)

@Serializable
data class RequestSubmitResponse(
    val success: Boolean? = null,
    val requestId: String? = null,
    val status: String? = null,
)

/** `status` settles into `resolved`/`rejected`/`failed`; a 404 means "unknown", per CLAUDE_CODE_PROMPT.md. */
@Serializable
data class RequestStatusResponse(
    val status: String? = null,
    val track: HistoryTrack? = null,
    val queuePosition: Int? = null,
)

interface SubwaveApi {
    @GET("health")
    suspend fun health(): Response<Unit>

    @GET("now-playing")
    suspend fun nowPlaying(): NowPlayingResponse

    @GET("state")
    suspend fun state(): StateResponse

    @GET("session")
    suspend fun session(): SessionResponse

    @GET("themes")
    suspend fun themes(): ThemesResponse

    @POST("request")
    suspend fun submitRequest(@Body body: RequestSubmitBody): RequestSubmitResponse

    @GET("request/{id}")
    suspend fun requestStatus(@Path("id") id: String): RequestStatusResponse

    /** Admin-only — requires [AdminAuthProvider], see [ApiClient.createAdmin]. */
    @POST("dj/skip")
    suspend fun skip(): SkipResponse

    /**
     * Admin-only, side-effect-free — used only to verify a username/password pair before saving
     * them (see [fr.m335.subtide.ui.admin.AdminViewModel.saveCredentials]); the stats payload
     * itself is out of scope v1, so the body is never parsed, just the 200-vs-401 status.
     */
    @GET("stats")
    suspend fun verifyAdminCredentials(): Response<Unit>
}

@Serializable
data class SkipResponse(
    val ok: Boolean? = null,
)
