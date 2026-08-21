package fr.m335.subtide.ui.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.m335.subtide.data.HistoryTrack
import fr.m335.subtide.data.RequestSubmitBody
import fr.m335.subtide.data.SubwaveApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

private val TERMINAL_STATUSES = setOf("resolved", "rejected", "failed")
private const val POLL_INTERVAL_MS = 3000L

data class RequestUiState(
    val input: String = "",
    val name: String = "",
    val isSubmitting: Boolean = false,
    val status: String? = null,
    val resolvedTrack: HistoryTrack? = null,
    val queuePosition: Int? = null,
    val errorMessage: String? = null,
)

/** `POST /request` then poll `GET /request/:id` until resolved/rejected/failed, per feature 6. */
class RequestViewModel(private val api: SubwaveApi) : ViewModel() {
    private val _uiState = MutableStateFlow(RequestUiState())
    val uiState: StateFlow<RequestUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null

    fun onInputChange(value: String) {
        _uiState.update { it.copy(input = value) }
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value) }
    }

    /** Fills the input from a tappable suggestion (see `RequestPage`'s "ON THE WIRE" tiles) — the user can still edit before sending. */
    fun applySuggestion(text: String) {
        _uiState.update { it.copy(input = text) }
    }

    fun submit() {
        val text = uiState.value.input.trim()
        if (text.isEmpty()) return
        val name = uiState.value.name.trim().ifEmpty { null }

        pollJob?.cancel()
        viewModelScope.launch {
            _uiState.update {
                it.copy(isSubmitting = true, errorMessage = null, status = null, resolvedTrack = null, queuePosition = null)
            }
            try {
                val response = api.submitRequest(RequestSubmitBody(text = text, name = name))
                val id = response.requestId
                if (id != null) {
                    _uiState.update { it.copy(isSubmitting = false, status = response.status ?: "pending") }
                    startPolling(id)
                } else {
                    _uiState.update { it.copy(isSubmitting = false, errorMessage = "La requête n'a pas été acceptée") }
                }
            } catch (e: IOException) {
                _uiState.update { it.copy(isSubmitting = false, errorMessage = "Connexion impossible") }
            } catch (e: HttpException) {
                _uiState.update { it.copy(isSubmitting = false, errorMessage = "Erreur serveur (${e.code()})") }
            }
        }
    }

    private fun startPolling(id: String) {
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                try {
                    val response = api.requestStatus(id)
                    _uiState.update {
                        it.copy(status = response.status, resolvedTrack = response.track, queuePosition = response.queuePosition)
                    }
                    if (response.status in TERMINAL_STATUSES) break
                } catch (e: HttpException) {
                    if (e.code() == 404) {
                        _uiState.update { it.copy(status = "unknown") }
                        break
                    }
                } catch (e: IOException) {
                    // Transient network error — keep polling.
                }
            }
        }
    }

    fun reset() {
        pollJob?.cancel()
        _uiState.value = RequestUiState()
    }

    override fun onCleared() {
        pollJob?.cancel()
    }
}
