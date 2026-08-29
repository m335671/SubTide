package fr.m335.subtide.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.m335.subtide.data.NowPlayingCache
import fr.m335.subtide.data.NowPlayingResponse
import fr.m335.subtide.data.SessionResponse
import fr.m335.subtide.data.StateResponse
import fr.m335.subtide.data.SubwaveApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

sealed interface PlayerUiState {
    data object Loading : PlayerUiState
    data class Ready(
        val nowPlaying: NowPlayingResponse,
        val state: StateResponse,
        val session: SessionResponse,
        /** Wall-clock round-trip of the `/now-playing` call — a real network latency reading, not a stream buffer position. Null while showing a cached snapshot with no live call to time. */
        val latencyMs: Int?,
        /** True while these values are the last snapshot persisted by [NowPlayingCache], shown while the app re-establishes its own connection rather than fetched from a poll of this session. */
        val isFromCache: Boolean = false,
    ) : PlayerUiState
    data class Error(val message: String) : PlayerUiState
}

private const val POLL_INTERVAL_MS = 5000L

/** Polls `/now-playing` + `/state` + `/session` every 5s, per CLAUDE_CODE_PROMPT.md's step 2. */
class PlayerViewModel(private val api: SubwaveApi, private val cache: NowPlayingCache) : ViewModel() {
    private val _uiState = MutableStateFlow(initialUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private fun initialUiState(): PlayerUiState {
        val cached = cache.load() ?: return PlayerUiState.Loading
        return PlayerUiState.Ready(
            nowPlaying = cached.nowPlaying,
            state = cached.state,
            session = cached.session,
            latencyMs = null,
            isFromCache = true,
        )
    }

    init {
        viewModelScope.launch {
            while (isActive) {
                poll()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun poll() {
        try {
            val start = System.currentTimeMillis()
            val nowPlaying = api.nowPlaying()
            val latencyMs = (System.currentTimeMillis() - start).toInt()
            val state = api.state()
            val session = api.session()
            cache.save(nowPlaying, state, session)
            _uiState.value = PlayerUiState.Ready(nowPlaying, state, session, latencyMs)
        } catch (e: IOException) {
            onPollFailed("Impossible de contacter le serveur")
        } catch (e: HttpException) {
            onPollFailed("Erreur serveur (${e.code()})")
        }
    }

    /** Keeps the last known-good (fresh or cached) values on screen through a failed poll instead of
     * blanking them out — only surfaces [PlayerUiState.Error] when there's nothing to show yet. */
    private fun onPollFailed(message: String) {
        if (_uiState.value !is PlayerUiState.Ready) {
            _uiState.value = PlayerUiState.Error(message)
        }
    }
}
