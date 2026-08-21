package fr.m335.subtide.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.m335.subtide.data.AdminCredentials
import fr.m335.subtide.ui.admin.AdminViewModel
import fr.m335.subtide.ui.theme.SubTideTheme

/** "Réglages avancés / Admin" — save/forget credentials, skip with a two-tap confirm, per feature 9. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDrawer(credentials: AdminCredentials, rootUrl: String, onDismiss: () -> Unit) {
    val viewModel: AdminViewModel = viewModel(
        factory = viewModelFactory { initializer { AdminViewModel(credentials, rootUrl) } },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SubTideTheme.colors
    val derived = SubTideTheme.derivedColors
    val type = SubTideTheme.typography
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RectangleShape,
        containerColor = colors.bg,
        contentColor = colors.ink,
        dragHandle = null,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(text = "ADMIN", style = type.eyebrow, color = colors.muted)
            Spacer(Modifier.height(16.dp))

            if (uiState.hasSavedCredentials) {
                Text(
                    text = "CONNECTED AS ${uiState.savedUsername?.uppercase() ?: "ADMIN"}",
                    style = type.monoLabel,
                    color = colors.muted,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Skipping the current track now lives in the SKIP tab.",
                    style = type.bodyRegular,
                    color = colors.muted,
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "DISCONNECT",
                    style = type.eyebrow,
                    color = derived.destructive,
                    modifier = Modifier.clickable(onClick = viewModel::forgetCredentials),
                )
            } else {
                AdminField(
                    label = "USERNAME",
                    value = uiState.usernameInput,
                    onValueChange = viewModel::onUsernameChange,
                    isPassword = false,
                )
                Spacer(Modifier.height(12.dp))
                AdminField(
                    label = "PASSWORD",
                    value = uiState.passwordInput,
                    onValueChange = viewModel::onPasswordChange,
                    isPassword = true,
                    onDone = viewModel::saveCredentials,
                )
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .border(BorderStroke(1.dp, colors.accent))
                        .clickable(enabled = !uiState.isVerifying, onClick = viewModel::saveCredentials),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (uiState.isVerifying) "VERIFYING…" else "SAVE",
                        style = type.eyebrow,
                        color = colors.accent,
                    )
                }
                uiState.loginError?.let { error ->
                    Text(
                        text = error,
                        style = type.monoLabel,
                        color = derived.destructive,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AdminField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isPassword: Boolean,
    onDone: (() -> Unit)? = null,
) {
    val colors = SubTideTheme.colors
    val derived = SubTideTheme.derivedColors
    val type = SubTideTheme.typography

    Text(text = label, style = type.eyebrow, color = colors.muted)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(colors.field)
            .border(BorderStroke(1.dp, derived.line))
            .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = type.bodyRegular.copy(color = colors.ink),
            singleLine = true,
            cursorBrush = SolidColor(colors.accent),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text,
                imeAction = if (onDone != null) ImeAction.Done else ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
