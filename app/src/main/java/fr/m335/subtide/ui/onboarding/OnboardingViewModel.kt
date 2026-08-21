package fr.m335.subtide.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.m335.subtide.data.ApiClient
import fr.m335.subtide.data.ListenerAccess
import fr.m335.subtide.data.ServerPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

data class OnboardingUiState(
    val urlInput: String = "",
    val accessKeyInput: String = "",
    val isChecking: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * Onboarding step 1 — validates a server URL against `GET /health` before persisting it. The
 * access key is optional: most stations don't need one, but some private ones gate the actual
 * audio stream behind `?auth=<key>` even though `/health` and the rest of the JSON API stay public
 * (see [ListenerAccess]) — so it's saved alongside the URL without being part of the health check.
 */
class OnboardingViewModel(
    private val serverPreferences: ServerPreferences,
    private val listenerAccess: ListenerAccess,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onUrlChange(value: String) {
        _uiState.update { it.copy(urlInput = value, errorMessage = null) }
    }

    fun onAccessKeyChange(value: String) {
        _uiState.update { it.copy(accessKeyInput = value) }
    }

    fun onConnectClick() {
        val typed = uiState.value.urlInput.trim().trimEnd('/')
        if (typed.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Adresse du serveur requise") }
            return
        }
        val normalizedUrl = if (typed.startsWith("http://") || typed.startsWith("https://")) {
            typed
        } else {
            "https://$typed"
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isChecking = true, errorMessage = null) }
            try {
                val response = ApiClient.create(normalizedUrl).health()
                if (response.isSuccessful) {
                    listenerAccess.accessKey = uiState.value.accessKeyInput.trim()
                    serverPreferences.setBaseUrl(normalizedUrl)
                } else {
                    _uiState.update {
                        it.copy(isChecking = false, errorMessage = "Le serveur a répondu ${response.code()}")
                    }
                }
            } catch (e: IOException) {
                _uiState.update {
                    it.copy(isChecking = false, errorMessage = "Connexion impossible — vérifie l'adresse")
                }
            }
        }
    }
}
