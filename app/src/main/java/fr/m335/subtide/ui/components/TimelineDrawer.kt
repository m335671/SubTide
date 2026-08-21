package fr.m335.subtide.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.m335.subtide.data.HistoryTrack
import fr.m335.subtide.ui.theme.SubTideTheme
import fr.m335.subtide.ui.util.formatStationTime

/** Historique des morceaux joués — dense text list, mono metadata, no cards, per design-system.md §5.4. */
@Composable
fun TimelinePage(tracks: List<HistoryTrack>, timezone: String?, modifier: Modifier = Modifier) {
    val colors = SubTideTheme.colors
    val derived = SubTideTheme.derivedColors
    val type = SubTideTheme.typography

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(text = "TIMELINE", style = type.eyebrow, color = colors.muted)
        Spacer(Modifier.height(12.dp))
        if (tracks.isEmpty()) {
            Text(text = "scanning the dial_", style = type.displayLight, color = derived.inkFaint)
        } else {
            LazyColumn {
                itemsIndexed(tracks) { index, track ->
                    TimelineRow(track = track, timezone = timezone)
                    if (index != tracks.lastIndex) {
                        HorizontalDivider(color = derived.line, thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineRow(track: HistoryTrack, timezone: String?) {
    val colors = SubTideTheme.colors
    val type = SubTideTheme.typography
    val subtitle = listOfNotNull(track.artist, track.album).joinToString(" · ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatStationTime(track.startedAt, timezone),
            style = type.monoLabel,
            color = colors.muted,
            modifier = Modifier.width(52.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title ?: "—",
                style = type.bodyRegular,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = type.monoLabel,
                    color = colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
