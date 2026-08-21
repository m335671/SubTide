package fr.m335.subtide.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.m335.subtide.data.BoothMessage
import fr.m335.subtide.ui.theme.SubTideTheme
import fr.m335.subtide.ui.util.formatStationTime

private enum class BoothFilter(val label: String) {
    ALL("ALL"),
    DJ("DJ"),
    TRACKS("TRACKS"),
}

/** Ce que le DJ IA a dit/pensé sur l'antenne — filtered transcript, per design-system.md §5.4/§6. */
@Composable
fun BoothPage(messages: List<BoothMessage>, timezone: String?, modifier: Modifier = Modifier) {
    val colors = SubTideTheme.colors
    val derived = SubTideTheme.derivedColors
    val type = SubTideTheme.typography
    var filter by remember { mutableStateOf(BoothFilter.ALL) }

    val visible = messages
        .asReversed()
        .filter { it.kind != "system" }
        .filter { message ->
            when (filter) {
                BoothFilter.ALL -> true
                BoothFilter.DJ -> message.role == "segment" || message.role == "dj"
                BoothFilter.TRACKS -> message.role == "track"
            }
        }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(text = "BOOTH", style = type.eyebrow, color = colors.muted)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BoothFilter.entries.forEach { entry ->
                FilterChip(
                    label = entry.label,
                    selected = filter == entry,
                    onClick = { filter = entry },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        if (visible.isEmpty()) {
            Text(text = "scanning the dial_", style = type.displayLight, color = derived.inkFaint)
        } else {
            LazyColumn {
                items(visible) { message ->
                    BoothRow(message = message, timezone = timezone)
                    HorizontalDivider(color = derived.line, thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = SubTideTheme.colors
    val derived = SubTideTheme.derivedColors
    val type = SubTideTheme.typography
    Box(
        modifier = Modifier
            .border(1.dp, if (selected) colors.accent else derived.line)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = type.eyebrow,
            color = if (selected) colors.accent else colors.muted,
        )
    }
}

@Composable
private fun BoothRow(message: BoothMessage, timezone: String?) {
    val colors = SubTideTheme.colors
    val type = SubTideTheme.typography
    val isVoice = message.role == "segment"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = formatStationTime(message.t, timezone),
            style = type.monoLabel,
            color = colors.muted,
            modifier = Modifier.width(52.dp),
        )
        Text(
            text = message.text ?: "",
            style = type.bodyRegular,
            color = if (isVoice) colors.accent else colors.muted,
            modifier = Modifier.weight(1f),
        )
    }
}
