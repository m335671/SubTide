package fr.m335.subtide.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.m335.subtide.data.AdminCredentials
import fr.m335.subtide.data.ApiClient
import fr.m335.subtide.data.ListenerAccess
import fr.m335.subtide.data.ServerPreferences
import fr.m335.subtide.data.ThemePreferences
import fr.m335.subtide.data.toThemeOptions
import fr.m335.subtide.ui.onboarding.OnboardingScreen
import fr.m335.subtide.ui.screens.PlayerScreen
import fr.m335.subtide.ui.theme.SubTideTheme
import fr.m335.subtide.ui.theme.SubwaveThemeDefaults
import kotlinx.coroutines.flow.map

/** Distinguishes "the persisted base URL hasn't loaded yet" from "loaded, and there isn't one" —
 * collapsing both into a plain nullable `String?` made [SubTideApp] flash [OnboardingScreen] for a
 * moment on every launch, even when a server was already configured, while DataStore delivered its
 * first real value. */
private sealed interface BaseUrlState {
    data object Loading : BaseUrlState
    data class Resolved(val url: String?) : BaseUrlState
}

/**
 * Root of the app — resolves the active [fr.m335.subtide.ui.theme.SubwaveThemeOption] (server
 * catalog + locally persisted choice, per feature 5) before rendering either onboarding step 1
 * (server URL) or the live [PlayerScreen], since [SubTideTheme] has to wrap everything. The theme
 * catalog is fetched via a plain [LaunchedEffect] rather than a ViewModel — it doesn't need to
 * survive configuration changes, and a `viewModel(key=, factory=)` at this composable level was
 * observed getting its scope cancelled milliseconds after creation on some launches.
 */
@Composable
fun SubTideApp() {
    val context = LocalContext.current.applicationContext
    val serverPreferences = remember { ServerPreferences(context) }
    val listenerAccess = remember { ListenerAccess(context) }
    val themePreferences = remember { ThemePreferences(context) }
    val adminCredentials = remember { AdminCredentials(context) }
    val baseUrlFlow = remember(serverPreferences) { serverPreferences.baseUrl.map { BaseUrlState.Resolved(it) as BaseUrlState } }
    val baseUrlState by baseUrlFlow.collectAsStateWithLifecycle(initialValue = BaseUrlState.Loading)
    val selectedThemeId by themePreferences.selectedThemeId.collectAsStateWithLifecycle(initialValue = null)
    val systemDark = isSystemInDarkTheme()
    val fallbackTheme = if (systemDark) SubwaveThemeDefaults.midnight else SubwaveThemeDefaults.classicLight

    val resolvedBaseUrl = baseUrlState as? BaseUrlState.Resolved
    val currentBaseUrl = resolvedBaseUrl?.url
    if (resolvedBaseUrl == null) {
        SubTideTheme(themeOption = fallbackTheme) {
            Box(modifier = Modifier.fillMaxSize().background(SubTideTheme.colors.bg))
        }
    } else if (currentBaseUrl == null) {
        SubTideTheme(themeOption = fallbackTheme) {
            OnboardingScreen(serverPreferences = serverPreferences, listenerAccess = listenerAccess)
        }
    } else {
        var themeCatalog by remember { mutableStateOf(SubwaveThemeDefaults.all) }
        var refreshTrigger by remember { mutableIntStateOf(0) }

        LaunchedEffect(currentBaseUrl, refreshTrigger) {
            val options = try {
                ApiClient.create(currentBaseUrl).themes().toThemeOptions()
            } catch (e: Exception) {
                emptyList()
            }
            if (options.isNotEmpty()) {
                themeCatalog = options
            }
        }

        // With no explicit user pick, prefer the server's "Classic Dark"/"Classic Light" entry
        // over whichever theme merely happens to be first in the catalog for that mode (e.g. a
        // "Blueprint" dark theme sorted ahead of it).
        val activeTheme = themeCatalog.find { it.id == selectedThemeId }
            ?: themeCatalog.firstOrNull { it.mode == fallbackTheme.mode && it.name.contains("classic", ignoreCase = true) }
            ?: themeCatalog.firstOrNull { it.mode == fallbackTheme.mode }
            ?: fallbackTheme

        SubTideTheme(themeOption = activeTheme) {
            PlayerScreen(
                baseUrl = currentBaseUrl,
                serverPreferences = serverPreferences,
                themePreferences = themePreferences,
                themeCatalog = themeCatalog,
                onRefreshThemes = { refreshTrigger++ },
                adminCredentials = adminCredentials,
            )
        }
    }
}
