package fr.m335.subtide.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.m335.subtide.data.AdminCredentials
import fr.m335.subtide.ui.admin.AdminViewModel
import fr.m335.subtide.ui.theme.SubTideTheme

/**
 * Dedicated pager tab for feature 9 — only reachable once admin credentials are already saved
 * (see [PAGE_LABELS_BASE] gating in `PlayerScreen`), so there's nothing to gate here: by the time
 * this composes, [AdminCredentials.hasCredentials] is already true. Shares the same [AdminViewModel]
 * instance as `AdminDrawer` (login/logout stays there, per the user's request) via Compose's default
 * `viewModel()` scoping.
 */
@Composable
fun SkipPage(credentials: AdminCredentials, rootUrl: String, modifier: Modifier = Modifier) {
    val viewModel: AdminViewModel = viewModel(
        factory = viewModelFactory { initializer { AdminViewModel(credentials, rootUrl) } },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SubTideTheme.colors
    val derived = SubTideTheme.derivedColors
    val type = SubTideTheme.typography
    var awaitingConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "SKIP", style = type.eyebrow, color = colors.muted)
        Text(
            text = "Connected as ${uiState.savedUsername?.uppercase() ?: "ADMIN"}",
            style = type.bodyRegular,
            color = colors.muted,
        )
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(BorderStroke(1.dp, if (awaitingConfirm) derived.destructive else colors.accent))
                .clickable(enabled = !uiState.isSkipping) {
                    if (awaitingConfirm) {
                        viewModel.skip()
                        awaitingConfirm = false
                    } else {
                        awaitingConfirm = true
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = when {
                    uiState.isSkipping -> "SKIPPING…"
                    awaitingConfirm -> "TAP AGAIN TO CONFIRM"
                    else -> "SKIP CURRENT TRACK"
                },
                style = type.eyebrow,
                color = if (awaitingConfirm) derived.destructive else colors.accent,
            )
        }
        uiState.skipMessage?.let { message ->
            Text(
                text = message,
                style = type.monoLabel,
                color = if (uiState.skipMessageIsError) derived.destructive else colors.muted,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
