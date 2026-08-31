package fr.m335.subtide.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.m335.subtide.data.AdminCredentials
import fr.m335.subtide.data.ApiClient
import fr.m335.subtide.data.NowPlayingCache
import fr.m335.subtide.data.NowPlayingResponse
import fr.m335.subtide.data.ServerPreferences
import fr.m335.subtide.data.SessionResponse
import fr.m335.subtide.data.StateResponse
import fr.m335.subtide.data.ThemePreferences
import fr.m335.subtide.playback.PlaybackViewModel
import fr.m335.subtide.ui.components.AdminDrawer
import fr.m335.subtide.ui.components.BoothPage
import fr.m335.subtide.ui.components.CenterStage
import fr.m335.subtide.ui.components.DjThinkingTicker
import fr.m335.subtide.ui.components.RequestPage
import fr.m335.subtide.ui.components.SettingsMenuDrawer
import fr.m335.subtide.ui.components.SkipPage
import fr.m335.subtide.ui.components.ThemesDrawer
import fr.m335.subtide.ui.components.TimelinePage
import fr.m335.subtide.ui.components.TopBar
import fr.m335.subtide.ui.components.TransportBar
import fr.m335.subtide.ui.components.TuningDial
import fr.m335.subtide.ui.player.PlayerUiState
import fr.m335.subtide.ui.player.PlayerViewModel
import fr.m335.subtide.ui.theme.SubTideTheme
import fr.m335.subtide.ui.theme.SubwaveThemeOption
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val PAGE_TIMELINE = 0
private const val PAGE_LIVE = 1
private const val PAGE_BOOTH = 2
private const val PAGE_REQUEST = 3
private const val PAGE_SKIP = 4
private val PAGE_LABELS_BASE = listOf("TML", "LIVE", "BTH", "REQ")

/**
 * The main screen — a radio-dial-style [HorizontalPager] (Timeline / Live / Booth / Request) with
 * a [TuningDial] page indicator under the TopBar, replacing modal drawers per the user's reference
 * screenshot. TopBar/TransportBar stay docked; only the middle content swipes. TransportBar's
 * power/volume/mute are driven by the actual ExoPlayer session (see [PlaybackViewModel]) — the disc
 * mark up top reflects the station's broadcast status (`streamOnline`), while the Power ring
 * reflects whether *this device* is playing the stream. The settings gear still opens a small menu
 * hub (Themes / Change server) since those aren't "content pages".
 */
@Composable
fun PlayerScreen(
    baseUrl: String,
    serverPreferences: ServerPreferences,
    themePreferences: ThemePreferences,
    themeCatalog: List<SubwaveThemeOption>,
    onRefreshThemes: () -> Unit,
    adminCredentials: AdminCredentials,
    modifier: Modifier = Modifier,
) {
    val colors = SubTideTheme.colors
    val context = LocalContext.current.applicationContext
    val api = remember(baseUrl) { ApiClient.create(baseUrl) }
    val nowPlayingCache = remember { NowPlayingCache(context) }
    val viewModel: PlayerViewModel = viewModel(
        key = baseUrl,
        factory = viewModelFactory { initializer { PlayerViewModel(api, nowPlayingCache) } },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackViewModel: PlaybackViewModel = viewModel()
    val playback by playbackViewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val isAdminAuthed by adminCredentials.hasCredentialsFlow.collectAsStateWithLifecycle()
    val pageLabels = if (isAdminAuthed) PAGE_LABELS_BASE + "SKIP" else PAGE_LABELS_BASE
    val pagerState = rememberPagerState(initialPage = PAGE_LIVE) { pageLabels.size }

    var isLiked by remember { mutableStateOf(false) }
    var lastVolumeBeforeMute by remember { mutableFloatStateOf(1f) }
    var showThemes by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showAdmin by remember { mutableStateOf(false) }
    val selectedThemeId by themePreferences.selectedThemeId.collectAsStateWithLifecycle(initialValue = null)

    val response: NowPlayingResponse? = (uiState as? PlayerUiState.Ready)?.nowPlaying
    val state: StateResponse? = (uiState as? PlayerUiState.Ready)?.state
    val session: SessionResponse? = (uiState as? PlayerUiState.Ready)?.session
    val boothMessages = session?.messages ?: emptyList()
    val latestDjLine = boothMessages
        .asReversed()
        .firstOrNull { it.kind != "system" && (it.role == "segment" || it.role == "dj") }
        ?.text
    val track = response?.nowPlaying
    val isStreamOnline = response?.streamOnline ?: false
    val isStandby = state?.streamIdle == true
    val hostLabel = baseUrl.removePrefix("https://").removePrefix("http://")

    // The server only recalibrates `track.timestamp` every POLL_INTERVAL_MS via PlayerViewModel's
    // poll, but the on-screen clock ticks every second off the device's own wall clock in between —
    // ticking recomposition off nowMillis rather than off the poll keeps the display seamless
    // instead of jumping in 5s steps.
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(1000)
            nowMillis = System.currentTimeMillis()
        }
    }
    // The radio stops streaming when nobody is listening to save resources, so `track.timestamp`
    // can be left over from before the stream went idle — computing elapsed off it would then show
    // a runaway value like 85:23 against a 03:59 track. Once elapsed overshoots the track's own
    // duration, it's not a real position anymore, so reset it to 0 (the "starting up" state).
    val rawElapsedSec = track?.timestamp?.let { startedAt ->
        (nowMillis / 1000 - startedAt).toInt().coerceAtLeast(0)
    }
    val elapsedSec = rawElapsedSec?.let { raw ->
        val duration = track?.duration
        if (duration != null && raw > duration) 0 else raw
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        TopBar(
            stationName = response?.dj?.station ?: "Subwave",
            showName = null,
            djName = response?.dj?.name,
            tagline = if (isStandby) "ON STANDBY — WAITING FOR AUDITORS" else response?.dj?.tagline ?: hostLabel,
            isTunedIn = isStreamOnline && !isStandby,
            hasActiveIndicator = false,
            onSettingsClick = { showMenu = true },
        )
        TuningDial(pagerState = pagerState, labels = pageLabels)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { page ->
            when (page) {
                PAGE_TIMELINE -> TimelinePage(
                    tracks = state?.history ?: emptyList(),
                    timezone = state?.timezone,
                    modifier = Modifier.fillMaxSize(),
                )
                PAGE_BOOTH -> BoothPage(
                    messages = boothMessages,
                    timezone = state?.timezone,
                    modifier = Modifier.fillMaxSize(),
                )
                PAGE_REQUEST -> RequestPage(api = api, currentArtist = track?.artist, modifier = Modifier.fillMaxSize())
                PAGE_SKIP -> SkipPage(credentials = adminCredentials, rootUrl = baseUrl, modifier = Modifier.fillMaxSize())
                else -> Column(
                    modifier = Modifier.fillMaxSize().fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                ) {
                    CenterStage(
                        title = track?.title ?: "scanning the dial_",
                        artist = track?.artist,
                        album = track?.album,
                        genre = track?.genre,
                        bpm = track?.bpm,
                        key = track?.musicalKey,
                        moodLine = track?.moods?.takeIf { it.isNotEmpty() }?.joinToString(" · "),
                        llmTokens = response?.llmTokens,
                        coverUrl = track?.subsonicId?.let { ApiClient.coverUrl(baseUrl, it) },
                        elapsedSec = elapsedSec,
                        durationSec = track?.duration,
                        isLiked = isLiked,
                        onLikeClick = { isLiked = !isLiked },
                        onCoverClick = { scope.launch { pagerState.animateScrollToPage(PAGE_TIMELINE) } },
                    )
                    when (val playerState = uiState) {
                        PlayerUiState.Loading -> DjThinkingTicker(text = "tuning in_", onClick = {})
                        is PlayerUiState.Error -> DjThinkingTicker(text = playerState.message, onClick = {}, isError = true)
                        is PlayerUiState.Ready -> if (isStandby) {
                            DjThinkingTicker(text = "The server is in standby mode — no one is listening", onClick = {})
                        } else if (playerState.isFromCache) {
                            DjThinkingTicker(text = "reconnecting to the server_", onClick = {})
                        } else {
                            latestDjLine?.let {
                                DjThinkingTicker(
                                    text = it,
                                    onClick = { scope.launch { pagerState.animateScrollToPage(PAGE_BOOTH) } },
                                )
                            }
                        }
                    }
                }
            }
        }
        TransportBar(
            isOnAir = playback.isPlaying,
            listeners = response?.listeners?.current ?: 0,
            latencyMs = (uiState as? PlayerUiState.Ready)?.latencyMs,
            isBuffering = playback.isBuffering,
            volume = playback.volume,
            isMuted = playback.volume == 0f,
            onPowerClick = playbackViewModel::togglePlayPause,
            onVolumeChange = { playbackViewModel.setVolume(it) },
            onMuteToggle = {
                if (playback.volume > 0f) {
                    lastVolumeBeforeMute = playback.volume
                    playbackViewModel.setVolume(0f)
                } else {
                    playbackViewModel.setVolume(lastVolumeBeforeMute)
                }
            },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )
    }

    if (showThemes) {
        ThemesDrawer(
            themes = themeCatalog,
            selectedId = selectedThemeId,
            onSelect = { theme ->
                scope.launch { themePreferences.setSelectedThemeId(theme.id) }
            },
            onRefresh = onRefreshThemes,
            onDismiss = { showThemes = false },
        )
    }
    if (showMenu) {
        SettingsMenuDrawer(
            onOpenThemes = {
                showMenu = false
                showThemes = true
            },
            onOpenAdmin = {
                showMenu = false
                showAdmin = true
            },
            onChangeServer = {
                showMenu = false
                scope.launch { serverPreferences.clearBaseUrl() }
            },
            onDismiss = { showMenu = false },
        )
    }
    if (showAdmin) {
        AdminDrawer(
            credentials = adminCredentials,
            rootUrl = baseUrl,
            onDismiss = { showAdmin = false },
        )
    }
}
