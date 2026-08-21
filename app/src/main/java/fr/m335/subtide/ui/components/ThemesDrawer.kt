package fr.m335.subtide.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.m335.subtide.ui.theme.SubTideTheme
import fr.m335.subtide.ui.theme.SubwaveThemeOption

/** Theme gallery — applied immediately on tap, persisted locally, per design-system.md §2/§6. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemesDrawer(
    themes: List<SubwaveThemeOption>,
    selectedId: String?,
    onSelect: (SubwaveThemeOption) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "THEMES", style = type.eyebrow, color = colors.muted)
                Text(
                    text = "↻ REFRESH",
                    style = type.eyebrow,
                    color = colors.accent,
                    modifier = Modifier.clickable(onClick = onRefresh),
                )
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn {
                items(themes) { theme ->
                    ThemeRow(theme = theme, isSelected = theme.id == selectedId, onClick = { onSelect(theme) })
                    HorizontalDivider(color = derived.line, thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
private fun ThemeRow(theme: SubwaveThemeOption, isSelected: Boolean, onClick: () -> Unit) {
    val colors = SubTideTheme.colors
    val type = SubTideTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThemeSwatch(theme = theme)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = theme.name, style = type.bodyRegular, color = colors.ink)
            if (theme.description.isNotEmpty()) {
                Text(
                    text = theme.description,
                    style = type.monoLabel,
                    color = colors.muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (isSelected) {
            Text(text = "✓", style = type.eyebrow, color = colors.accent)
        }
    }
}

@Composable
private fun ThemeSwatch(theme: SubwaveThemeOption) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(theme.colors.bg)
            .border(BorderStroke(1.dp, theme.colors.ink.copy(alpha = 0.3f))),
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .align(Alignment.BottomEnd)
                .background(theme.colors.accent),
        )
    }
}
