package fr.m335.subtide.playback

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import fr.m335.subtide.MainActivity
import fr.m335.subtide.data.AdminCredentials
import fr.m335.subtide.data.ApiClient
import fr.m335.subtide.data.BasicAuthProvider
import fr.m335.subtide.data.HistoryTrack
import fr.m335.subtide.data.ListenerAccess
import fr.m335.subtide.data.NowPlayingResponse
import fr.m335.subtide.data.RequestSubmitBody
import fr.m335.subtide.data.ServerPreferences
import fr.m335.subtide.data.SubwaveApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

private const val ROOT_ID = "root"
private const val LIVE_FOLDER_ID = "live-folder"
private const val LIVE_ID = "live"
private const val HISTORY_ID = "history"
private const val REQUESTS_ID = "requests"
private const val REQUEST_MEDIA_ID_PREFIX = "request:"
private const val SKIP_FOLDER_ID = "skip"
private const val SKIP_ACTION_ID = "skip-action"
private const val NOT_CONFIGURED_ID = "not-configured"
private const val MORE_LIKE_THIS_ID = "morelike"
private const val SKIP_CUSTOM_ACTION = "fr.m335.subtide.ACTION_SKIP"
private val SKIP_COMMAND = SessionCommand(SKIP_CUSTOM_ACTION, Bundle.EMPTY)

/**
 * Fixed request shortcuts for Android Auto — one-tap alternatives to typing, which is disallowed
 * while driving. A real voice-driven request is also supported (see [BrowseTreeCallback.onAddMediaItems]),
 * for when a canned phrase doesn't cover it. "More like this" is deliberately first — see
 * [BrowseTreeCallback.onAddMediaItems] for how its text is built dynamically from the current track.
 */
private val REQUEST_PRESETS = listOf(
    RequestPreset(MORE_LIKE_THIS_ID, "More like this", "Play something more like this"),
    RequestPreset("calmer", "Something calmer", "Something calmer, please"),
    RequestPreset("energy", "More energy", "Play something with more energy"),
    RequestPreset("surprise", "Surprise me", "Surprise me with something different"),
    RequestPreset("other", "Something else", "Play something else"),
    RequestPreset("older", "Something older", "Play something older"),
    RequestPreset("newer", "Something newer", "Play something more recent"),
    RequestPreset("deepcut", "A deep cut", "Play a deep cut, something less obvious"),
    RequestPreset("genre", "Switch genre", "Switch it up, a different genre please"),
)

private data class RequestPreset(val id: String, val label: String, val text: String)

/**
 * Owns the ExoPlayer instance and the system [MediaSession] (notification, lock screen) — the same
 * session Android Auto binds to via [onGetSession], per CLAUDE_CODE_PROMPT.md. Polls `/now-playing`
 * + `/state` itself so metadata and the browse tree keep updating even while the app UI isn't
 * visible. Root children are always browsable folders — Live, read-only History, Requests, and
 * (only once admin credentials are saved) Skip — since Android Auto renders each of the root's
 * browsable children as its own tab; a bare playable item there doesn't fit that model.
 */
class PlaybackService : MediaLibraryService() {
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaLibrarySession
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var streamUrlSet = false

    @Volatile private var currentBaseUrl: String? = null
    @Volatile private var currentApi: SubwaveApi? = null
    @Volatile private var currentHistory: List<HistoryTrack> = emptyList()
    private lateinit var adminCredentials: AdminCredentials

    override fun onCreate() {
        super.onCreate()
        adminCredentials = AdminCredentials(applicationContext)
        player = ExoPlayer.Builder(this)
            // Without requesting audio focus, ExoPlayer never asks the system for it — harmless
            // on a phone speaker, but a real Android Auto head unit gates whether it actually
            // opens the car's media audio channel on the app holding focus. This is why audio
            // worked on the Desktop Head Unit (routed through the PC's own audio, no such gate)
            // but was silent in an actual car.
            .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus= */ true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        mediaSession = MediaLibrarySession.Builder(this, player, BrowseTreeCallback())
            .setSessionActivity(sessionActivityPendingIntent())
            .build()
        startPolling()
        serviceScope.launch {
            // The Skip tab's presence in the root, and the Skip button on the Now Playing screen,
            // both depend on this — if it changes while a car session is already open (e.g.
            // logging in from the phone mid-drive), push a refresh instead of waiting for the
            // browser to reopen the app.
            adminCredentials.hasCredentialsFlow.collect {
                mediaSession.notifyChildrenChanged(ROOT_ID, rootChildCount(), null)
                mediaSession.setCustomLayout(customLayout())
            }
        }
    }

    /** Matches `onGetChildren(ROOT_ID)` — kept in sync here only for the `notifyChildrenChanged` hint. */
    private fun rootChildCount(): Int =
        if (currentBaseUrl == null) 1 else if (adminCredentials.hasCredentials) 4 else 3

    /**
     * Shown on the Now Playing screen (Android Auto, notification, lock screen) next to the
     * standard transport controls — admin-only, same gating as the Skip tab. Uses Media3's own
     * "next" icon rather than a custom one: Android Auto's playback template only reliably renders
     * its own curated icon set for these buttons, a custom bitmap risks being silently dropped.
     */
    private fun customLayout(): List<CommandButton> =
        if (adminCredentials.hasCredentials) {
            listOf(
                CommandButton.Builder(CommandButton.ICON_NEXT)
                    .setSessionCommand(SKIP_COMMAND)
                    .setDisplayName("Skip")
                    .setSlots(CommandButton.SLOT_FORWARD)
                    .build(),
            )
        } else {
            emptyList()
        }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession = mediaSession

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    private fun sessionActivityPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun startPolling() {
        val serverPreferences = ServerPreferences(applicationContext)
        serviceScope.launch {
            serverPreferences.baseUrl.distinctUntilChanged().collectLatest { baseUrl ->
                currentBaseUrl = baseUrl
                mediaSession.notifyChildrenChanged(ROOT_ID, rootChildCount(), null)
                if (baseUrl == null) {
                    currentApi = null
                    return@collectLatest
                }
                val api = ApiClient.create(baseUrl)
                currentApi = api
                streamUrlSet = false
                while (isActive) {
                    try {
                        updatePlayer(baseUrl, api.nowPlaying())
                    } catch (e: IOException) {
                        // Network hiccup — keep last known state, retry on the next tick.
                    } catch (e: HttpException) {
                        // Server error — same as above.
                    }
                    try {
                        val history = api.state().history ?: emptyList()
                        if (history != currentHistory) {
                            currentHistory = history
                            // Without this, a browser that already fetched the Recent History
                            // folder (e.g. Android Auto, right after binding) never re-queries
                            // it — it only shows whatever was there at that first, possibly
                            // still-empty, poll tick.
                            mediaSession.notifyChildrenChanged(HISTORY_ID, history.size, null)
                        }
                    } catch (e: IOException) {
                        // Keep the last known history.
                    } catch (e: HttpException) {
                        // Keep the last known history.
                    }
                    delay(5000)
                }
            }
        }
    }

    private fun updatePlayer(baseUrl: String, response: NowPlayingResponse) {
        val metadata = buildMetadata(baseUrl, response)
        val mount = response.stream?.mount
        if (!streamUrlSet && mount != null) {
            val accessKey = ListenerAccess(applicationContext).accessKey
            val streamUrl = ApiClient.rootRelativeUrl(baseUrl, mount, accessKey)
            player.setMediaItem(MediaItem.Builder().setUri(streamUrl).setMediaMetadata(metadata).build())
            player.prepare()
            streamUrlSet = true
            // The Live folder's only child is null (unavailable) until this point — a browser
            // that opened that tab before this resolves would otherwise never see it appear.
            mediaSession.notifyChildrenChanged(LIVE_FOLDER_ID, 1, null)
        } else {
            val currentItem = player.currentMediaItem ?: return
            player.replaceMediaItem(player.currentMediaItemIndex, currentItem.buildUpon().setMediaMetadata(metadata).build())
        }
    }

    private fun buildMetadata(baseUrl: String, response: NowPlayingResponse): MediaMetadata {
        val track = response.nowPlaying
        val artworkUri = track?.subsonicId?.let { Uri.parse(ApiClient.coverUrl(baseUrl, it)) }
        return MediaMetadata.Builder()
            .setTitle(track?.title ?: response.dj?.station ?: "Subwave")
            .setArtist(track?.artist)
            .setAlbumTitle(track?.album)
            .setArtworkUri(artworkUri)
            .build()
    }

    /** Shown as the only root child until a server URL is set (see `OnboardingScreen`) — otherwise
     *  Android Auto would just show a set of hollow, empty-looking folders with no explanation. */
    private fun notConfiguredItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(NOT_CONFIGURED_ID)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Radio not configured")
                    .setArtist("Open SUB/TIDE on your phone to set it up")
                    .setIsBrowsable(false)
                    .setIsPlayable(false)
                    .build(),
            )
            .build()

    private fun folderItem(id: String, title: String): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .build(),
            )
            .build()

    /** The live stream, playable straight from the browse tree — same URI/session as the app UI. */
    private fun liveItem(): MediaItem? {
        val baseUrl = currentBaseUrl ?: return null
        val existing = player.currentMediaItem ?: return null
        val liveMetadata = existing.mediaMetadata.buildUpon()
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
            .build()
        return existing.buildUpon().setMediaId(LIVE_ID).setMediaMetadata(liveMetadata).build()
    }

    private fun presetItem(preset: RequestPreset): MediaItem =
        MediaItem.Builder()
            .setMediaId(REQUEST_MEDIA_ID_PREFIX + preset.id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(preset.label)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .build(),
            )
            .build()

    private fun skipActionItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(SKIP_ACTION_ID)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Skip current track")
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .build(),
            )
            .build()

    /** Fire-and-forget — the car UI has no room to show pending/resolved status, unlike [RequestDrawer]. */
    private fun submitRequestText(text: String) {
        val api = currentApi ?: return
        serviceScope.launch {
            try {
                api.submitRequest(RequestSubmitBody(text = text))
            } catch (e: IOException) {
                // Best-effort — no UI to surface the failure to in the car.
            } catch (e: HttpException) {
                // Same as above.
            }
        }
    }

    /**
     * Admin-only, one tap, no confirmation dance — unlike [fr.m335.subtide.ui.components.SkipPage]'s
     * two-tap confirm, a browse-tree item isn't a great place for a stateful "tap again" affordance,
     * and the Skip tab only exists in the tree at all when credentials are already saved (see
     * [BrowseTreeCallback.onGetChildren]), so reaching it is already a deliberate action.
     */
    private fun performSkip() {
        val baseUrl = currentBaseUrl ?: return
        val username = adminCredentials.username ?: return
        val password = adminCredentials.password ?: return
        serviceScope.launch {
            try {
                ApiClient.createAdmin(baseUrl, BasicAuthProvider(username, password)).skip()
            } catch (e: IOException) {
                // Best-effort — no UI to surface the failure to in the car.
            } catch (e: HttpException) {
                // Same as above.
            }
        }
    }

    /** Read-only — a live radio stream has no seekable past, so history entries aren't playable. */
    private fun historyItem(track: HistoryTrack): MediaItem {
        val artworkUri = (currentBaseUrl to track.subsonicId).let { (baseUrl, id) ->
            if (baseUrl != null && id != null) Uri.parse(ApiClient.coverUrl(baseUrl, id)) else null
        }
        return MediaItem.Builder()
            .setMediaId("history:${track.subsonicId ?: track.title.orEmpty()}")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title ?: "—")
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .setArtworkUri(artworkUri)
                    .setIsBrowsable(false)
                    .setIsPlayable(false)
                    .build(),
            )
            .build()
    }

    private inner class BrowseTreeCallback : MediaLibrarySession.Callback {
        /** Registers [SKIP_COMMAND] as an available session command and seeds the initial custom layout. */
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
                .buildUpon()
                .add(SKIP_COMMAND)
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
                .setAvailableSessionCommands(sessionCommands)
                .setCustomLayout(customLayout())
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == SKIP_CUSTOM_ACTION) {
                performSkip()
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> =
            Futures.immediateFuture(LibraryResult.ofItem(folderItem(ROOT_ID, "SUB/WAVE"), params))

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val item = when (mediaId) {
                ROOT_ID -> folderItem(ROOT_ID, "SUB/WAVE")
                LIVE_FOLDER_ID -> folderItem(LIVE_FOLDER_ID, "Live")
                LIVE_ID -> liveItem()
                HISTORY_ID -> folderItem(HISTORY_ID, "History")
                REQUESTS_ID -> folderItem(REQUESTS_ID, "Requests")
                SKIP_FOLDER_ID -> folderItem(SKIP_FOLDER_ID, "Skip")
                SKIP_ACTION_ID -> skipActionItem()
                NOT_CONFIGURED_ID -> notConfiguredItem()
                else -> currentHistory.firstOrNull { "history:${it.subsonicId ?: it.title.orEmpty()}" == mediaId }
                    ?.let { historyItem(it) }
                    ?: REQUEST_PRESETS.firstOrNull { REQUEST_MEDIA_ID_PREFIX + it.id == mediaId }?.let { presetItem(it) }
            }
            return if (item != null) {
                Futures.immediateFuture(LibraryResult.ofItem(item, null))
            } else {
                Futures.immediateFuture(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
            }
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val children = when (parentId) {
                // Every root child is browsable (a tab) — Android Auto only tabs browsable items,
                // so a bare playable item here (the old, unwrapped Live) got shoved into an
                // unlabeled fallback tab instead. Wrapping Live in its own folder like the others
                // keeps the root uniform. No server URL yet? Show one explanatory item instead of
                // a set of hollow, empty-looking folders.
                ROOT_ID -> if (currentBaseUrl == null) {
                    listOf(notConfiguredItem())
                } else {
                    listOfNotNull(
                        folderItem(LIVE_FOLDER_ID, "Live"),
                        folderItem(HISTORY_ID, "History"),
                        folderItem(REQUESTS_ID, "Requests"),
                        folderItem(SKIP_FOLDER_ID, "Skip").takeIf { adminCredentials.hasCredentials },
                    )
                }
                LIVE_FOLDER_ID -> listOfNotNull(liveItem())
                HISTORY_ID -> currentHistory.map { historyItem(it) }
                REQUESTS_ID -> REQUEST_PRESETS.map { presetItem(it) }
                SKIP_FOLDER_ID -> if (adminCredentials.hasCredentials) listOf(skipActionItem()) else emptyList()
                else -> emptyList()
            }
            return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(children), params))
        }

        /**
         * Resolves browse-tree taps into real playable items — per the Media3 migration guide,
         * this is where a legacy `playFromMediaId` (what Android Auto sends on a tap) gets filled
         * in with an actual URI. A request preset or the Skip action has none: tapping fires the
         * corresponding side effect and hands back whatever is already playing, so the live stream
         * just keeps going.
         *
         * This is also where a hands-free voice request lands: "Hey Google, ask SubTide to play
         * something calmer" (or the car's own voice-assistant button) reaches the session as a
         * `playFromSearch`, which Media3 translates into a placeholder [MediaItem] carrying the
         * spoken text in `requestMetadata.searchQuery` rather than a `mediaId`.
         */
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
        ): ListenableFuture<List<MediaItem>> {
            val resolved = mediaItems.map { item ->
                val preset = REQUEST_PRESETS.firstOrNull { REQUEST_MEDIA_ID_PREFIX + it.id == item.mediaId }
                val searchQuery = item.requestMetadata.searchQuery?.trim()?.takeIf { it.isNotEmpty() }
                when {
                    preset != null -> {
                        val text = if (preset.id == MORE_LIKE_THIS_ID) {
                            player.currentMediaItem?.mediaMetadata?.artist?.let { "Play more like $it" } ?: preset.text
                        } else {
                            preset.text
                        }
                        submitRequestText(text)
                        player.currentMediaItem ?: item
                    }
                    searchQuery != null -> {
                        submitRequestText(searchQuery)
                        player.currentMediaItem ?: item
                    }
                    item.mediaId == SKIP_ACTION_ID -> {
                        performSkip()
                        player.currentMediaItem ?: item
                    }
                    item.mediaId == LIVE_ID -> liveItem() ?: item
                    else -> item
                }
            }
            return Futures.immediateFuture(resolved)
        }

        /**
         * The on-screen search icon, wired to submit whatever text comes back as a real request —
         * Android Auto disables its own keyboard while the car is in motion and falls back to
         * voice dictation through the same search bar, so this doesn't reopen the "typing while
         * driving" problem [onAddMediaItems]'s `searchQuery` path was already built to avoid.
         */
        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<Void>> {
            query.trim().takeIf { it.isNotEmpty() }?.let { submitRequestText(it) }
            session.notifySearchResultChanged(browser, query, 1, params)
            return Futures.immediateFuture(LibraryResult.ofVoid(params))
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val confirmation = MediaItem.Builder()
                .setMediaId("search-confirmation")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Request sent: “$query”")
                        .setIsBrowsable(false)
                        .setIsPlayable(false)
                        .build(),
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.of(confirmation), params))
        }

        /**
         * Called when a controller (Android Auto's own "resume" affordance, the lock screen, an
         * Assistant "play" request) asks to start playback without picking a browse item first —
         * without this, opening the app in the car just shows the empty-looking folder list
         * instead of the live stream's Now Playing screen.
         */
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            isForPlayback: Boolean,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val current = player.currentMediaItem
            val items = if (current != null) ImmutableList.of(current) else ImmutableList.of()
            return Futures.immediateFuture(MediaSession.MediaItemsWithStartPosition(items, 0, C.TIME_UNSET))
        }
    }
}
