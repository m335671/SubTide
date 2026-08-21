package fr.m335.subtide.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.m335.subtide.data.ListenerAccess
import fr.m335.subtide.data.ServerPreferences
import fr.m335.subtide.ui.theme.SubTideTheme

/** Onboarding step 1 — the very first screen, before any server is configured. */
@Composable
fun OnboardingScreen(
    serverPreferences: ServerPreferences,
    listenerAccess: ListenerAccess,
    modifier: Modifier = Modifier,
) {
    val viewModel: OnboardingViewModel = viewModel(
        factory = viewModelFactory { initializer { OnboardingViewModel(serverPreferences, listenerAccess) } },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SubTideTheme.colors
    val derived = SubTideTheme.derivedColors
    val type = SubTideTheme.typography

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "SUBWAVE", style = type.eyebrow, color = colors.ink)
        Text(
            text = "tune in to your station",
            style = type.displayLight,
            color = colors.ink,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
        )
        Text(text = "SERVER ADDRESS", style = type.eyebrow, color = colors.muted)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .background(colors.field)
                .border(BorderStroke(1.dp, derived.line))
                .padding(horizontal = 12.dp, vertical = 14.dp),
        ) {
            if (uiState.urlInput.isEmpty()) {
                Text(
                    text = "radio.example.com",
                    style = type.bodyRegular,
                    color = derived.inkFaint,
                )
            }
            BasicTextField(
                value = uiState.urlInput,
                onValueChange = viewModel::onUrlChange,
                textStyle = type.bodyRegular.copy(color = colors.ink),
                singleLine = true,
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { viewModel.onConnectClick() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            text = "ACCESS KEY (OPTIONAL)",
            style = type.eyebrow,
            color = colors.muted,
            modifier = Modifier.padding(top = 20.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .background(colors.field)
                .border(BorderStroke(1.dp, derived.line))
                .padding(horizontal = 12.dp, vertical = 14.dp),
        ) {
            if (uiState.accessKeyInput.isEmpty()) {
                Text(
                    text = "only if your station requires one",
                    style = type.bodyRegular,
                    color = derived.inkFaint,
                )
            }
            BasicTextField(
                value = uiState.accessKeyInput,
                onValueChange = viewModel::onAccessKeyChange,
                textStyle = type.bodyRegular.copy(color = colors.ink),
                singleLine = true,
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { viewModel.onConnectClick() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        uiState.errorMessage?.let { message ->
            Text(
                text = message,
                style = type.monoLabel,
                color = derived.destructive,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
                .height(48.dp)
                .border(BorderStroke(1.dp, colors.accent))
                .clickable(enabled = !uiState.isChecking, onClick = viewModel::onConnectClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (uiState.isChecking) "CONNECTING…" else "CONNECT",
                style = type.eyebrow,
                color = colors.accent,
            )
        }
    }
}
