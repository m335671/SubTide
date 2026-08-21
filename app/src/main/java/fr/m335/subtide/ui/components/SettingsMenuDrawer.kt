package fr.m335.subtide.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import fr.m335.subtide.ui.theme.SubTideTheme

/** Hub behind the TopBar's settings gear — reachable secondary features and "change server". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMenuDrawer(
    onOpenThemes: () -> Unit,
    onOpenAdmin: () -> Unit,
    onChangeServer: () -> Unit,
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
            MenuRow(label = "THEMES", color = colors.ink, onClick = onOpenThemes)
            HorizontalDivider(color = derived.line, thickness = 1.dp)
            MenuRow(label = "ADMIN", color = colors.ink, onClick = onOpenAdmin)
            HorizontalDivider(color = derived.line, thickness = 1.dp)
            MenuRow(label = "CHANGE SERVER", color = derived.destructive, onClick = onChangeServer)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MenuRow(label: String, color: Color, onClick: () -> Unit) {
    val type = SubTideTheme.typography
    Text(
        text = label,
        style = type.eyebrow,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
    )
}
