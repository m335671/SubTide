package fr.m335.subtide.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
        /** Wall-clock round-trip of the `/now-playing` call — a real network latency reading, not a stream buffer position. */
        val latencyMs: Int,
    ) : PlayerUiState
    data class Error(val message: String) : PlayerUiState
}

private const val POLL_INTERVAL_MS = 5000L

/** Polls `/now-playing` + `/state` + `/session` every 5s, per CLAUDE_CODE_PROMPT.md's step 2. */
class PlayerViewModel(private val api: SubwaveApi) : ViewModel() {
    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                poll()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun poll() {
        _uiState.value = try {
            val start = System.currentTimeMillis()
            val nowPlaying = api.nowPlaying()
            val latencyMs = (System.currentTimeMillis() - start).toInt()
            val state = api.state()
            val session = api.session()
            PlayerUiState.Ready(nowPlaying, state, session, latencyMs)
        } catch (e: IOException) {
            PlayerUiState.Error("Impossible de contacter le serveur")
        } catch (e: HttpException) {
            PlayerUiState.Error("Erreur serveur (${e.code()})")
        }
    }
}
