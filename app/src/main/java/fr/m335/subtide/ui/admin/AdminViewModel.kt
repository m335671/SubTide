package fr.m335.subtide.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.m335.subtide.data.AdminCredentials
import fr.m335.subtide.data.ApiClient
import fr.m335.subtide.data.BasicAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

data class AdminUiState(
    val usernameInput: String = "",
    val passwordInput: String = "",
    val hasSavedCredentials: Boolean = false,
    val savedUsername: String? = null,
    val isVerifying: Boolean = false,
    val loginError: String? = null,
    val isSkipping: Boolean = false,
    val skipMessage: String? = null,
    val skipMessageIsError: Boolean = false,
)

/** Admin panel — save/forget credentials and skip the current track, per feature 9. */
class AdminViewModel(
    private val credentials: AdminCredentials,
    private val rootUrl: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AdminUiState(hasSavedCredentials = credentials.hasCredentials, savedUsername = credentials.username),
    )
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    fun onUsernameChange(value: String) {
        _uiState.update { it.copy(usernameInput = value) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(passwordInput = value) }
    }

    /** Verifies the pair against the server before persisting anything — a typo used to only surface as a 401 later, at Skip time. */
    fun saveCredentials() {
        val username = uiState.value.usernameInput.trim()
        val password = uiState.value.passwordInput
        if (username.isEmpty() || password.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isVerifying = true, loginError = null) }
            try {
                ApiClient.createAdmin(rootUrl, BasicAuthProvider(username, password)).verifyAdminCredentials()
                credentials.save(username, password)
                _uiState.update {
                    it.copy(
                        isVerifying = false,
                        hasSavedCredentials = true,
                        savedUsername = username,
                        usernameInput = "",
                        passwordInput = "",
                        skipMessage = null,
                    )
                }
            } catch (e: HttpException) {
                val message = if (e.code() == 401) "Invalid admin credentials" else "Server error (${e.code()})"
                _uiState.update { it.copy(isVerifying = false, loginError = message) }
            } catch (e: IOException) {
                _uiState.update { it.copy(isVerifying = false, loginError = "Connection failed") }
            }
        }
    }

    fun forgetCredentials() {
        credentials.clear()
        _uiState.update { it.copy(hasSavedCredentials = false, savedUsername = null, skipMessage = null) }
    }

    fun skip() {
        val username = credentials.username ?: return
        val password = credentials.password ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSkipping = true, skipMessage = null) }
            try {
                val api = ApiClient.createAdmin(rootUrl, BasicAuthProvider(username, password))
                api.skip()
                _uiState.update { it.copy(isSkipping = false, skipMessage = "Skipped.", skipMessageIsError = false) }
            } catch (e: HttpException) {
                val message = when (e.code()) {
                    401 -> "Invalid admin credentials"
                    429 -> "Too many attempts — locked out, try again later"
                    else -> "Server error (${e.code()})"
                }
                _uiState.update { it.copy(isSkipping = false, skipMessage = message, skipMessageIsError = true) }
            } catch (e: IOException) {
                _uiState.update {
                    it.copy(isSkipping = false, skipMessage = "Connection failed", skipMessageIsError = true)
                }
            }
        }
    }
}
