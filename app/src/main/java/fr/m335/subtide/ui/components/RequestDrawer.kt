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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.m335.subtide.data.SubwaveApi
import fr.m335.subtide.ui.request.RequestUiState
import fr.m335.subtide.ui.request.RequestViewModel
import fr.m335.subtide.ui.theme.SubTideTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private data class Suggestion(val label: String, val subtitle: String, val text: String)

/** "ON THE WIRE" tiles — one dynamic (current track), two time-of-day-driven (no weather API to
 *  back a real "sunny afternoon" tile), one random, matching the reference screen's 2x2 layout. */
private fun buildSuggestions(currentArtist: String?): List<Suggestion> {
    val now = LocalTime.now()
    val (moodLabel, moodText) = when (now.hour) {
        in 5..10 -> "Morning energy" to "Something to start the day with energy"
        in 11..16 -> "Afternoon vibes" to "Something for the afternoon"
        in 17..21 -> "Wind down vibes" to "Something to wind down to"
        else -> "Late-night vibes" to "Something for late-night listening"
    }
    val clockLabel = now.format(DateTimeFormatter.ofPattern("h:mm a"))
    return listOf(
        Suggestion(
            label = "More like this",
            subtitle = currentArtist?.uppercase() ?: "SIMILAR",
            text = currentArtist?.let { "Play more like $it" } ?: "Play something more like this",
        ),
        Suggestion(label = moodLabel, subtitle = "RIGHT NOW", text = moodText),
        Suggestion(label = "On the clock", subtitle = clockLabel, text = "Play something that fits right now"),
        Suggestion(label = "Surprise me", subtitle = "RANDOM", text = "Surprise me with something different"),
    )
}

/**
 * "Make a request" — free-text song request → status tracking, per design-system.md §5.4 /
 * feature 6, reproducing the reference screen: a "Dear DJ —" note framed like an actual request
 * slip, an optional signature, and four tappable suggestions above the send button.
 */
@Composable
fun RequestPage(api: SubwaveApi, currentArtist: String? = null, modifier: Modifier = Modifier) {
    val viewModel: RequestViewModel = viewModel(
        factory = viewModelFactory { initializer { RequestViewModel(api) } },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SubTideTheme.colors
    val derived = SubTideTheme.derivedColors
    val type = SubTideTheme.typography
    val suggestions = buildSuggestions(currentArtist)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = "Make a request", style = type.displayTitle, color = colors.ink, modifier = Modifier.weight(1f))
            Text(text = "TO THE BOOTH", style = type.eyebrow, color = colors.muted)
        }
        Spacer(Modifier.height(10.dp))
        HorizontalRule(color = derived.line)
        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, derived.line)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.field)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).background(colors.accent, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(text = "LINE OPEN", style = type.monoLabel, color = colors.muted)
                }
                Text(text = "REQUEST SLIP", style = type.monoLabel, color = colors.muted)
            }
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "DEAR DJ —", style = type.monoLabel, color = colors.muted)
                Spacer(Modifier.height(14.dp))
                Row {
                    Box(modifier = Modifier.width(2.dp).height(60.dp).background(colors.accent))
                    Spacer(Modifier.width(14.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (uiState.input.isEmpty()) {
                            Text(
                                text = "play me something for late-night driving…",
                                style = type.bodyRegular,
                                color = derived.inkFaint,
                            )
                        }
                        BasicTextField(
                            value = uiState.input,
                            onValueChange = viewModel::onInputChange,
                            textStyle = type.bodyRegular.copy(color = colors.ink),
                            cursorBrush = SolidColor(colors.accent),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { viewModel.submit() }),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                HorizontalRule(color = derived.line)
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "— ", style = type.bodyRegular, color = derived.inkFaint)
                    Box(modifier = Modifier.weight(1f)) {
                        if (uiState.name.isEmpty()) {
                            Text(
                                text = "signed, your name (optional)",
                                style = type.bodyRegular,
                                color = derived.inkFaint,
                            )
                        }
                        BasicTextField(
                            value = uiState.name,
                            onValueChange = viewModel::onNameChange,
                            textStyle = type.bodyRegular.copy(color = colors.ink),
                            singleLine = true,
                            cursorBrush = SolidColor(colors.accent),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { viewModel.submit() }),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Describe a mood, a memory, an artist. The agentic AI DJ reads your note, digs the library, and answers you on-air.",
            style = type.bodyRegular,
            color = colors.muted,
        )
        Spacer(Modifier.height(24.dp))
        Text(text = "ON THE WIRE", style = type.eyebrow, color = colors.muted)
        Spacer(Modifier.height(12.dp))
        for (row in suggestions.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { suggestion ->
                    SuggestionTile(
                        suggestion = suggestion,
                        onClick = { viewModel.applySuggestion(suggestion.text) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(colors.accent)
                .clickable(enabled = !uiState.isSubmitting, onClick = viewModel::submit),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (uiState.isSubmitting) "SENDING…" else "SEND TO THE BOOTH ↗",
                style = type.eyebrow,
                color = colors.bg,
            )
        }
        uiState.errorMessage?.let { message ->
            Text(
                text = message,
                style = type.monoLabel,
                color = derived.destructive,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        uiState.status?.let { status ->
            Spacer(Modifier.height(20.dp))
            RequestStatusRow(status = status, uiState = uiState)
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun SuggestionTile(suggestion: Suggestion, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = SubTideTheme.colors
    val derived = SubTideTheme.derivedColors
    val type = SubTideTheme.typography
    Column(
        modifier = modifier
            .border(BorderStroke(1.dp, derived.line))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(text = suggestion.label, style = type.bodyRegular.copy(fontWeight = FontWeight.Medium), color = colors.ink)
        Spacer(Modifier.height(4.dp))
        Text(text = suggestion.subtitle, style = type.monoLabel, color = colors.muted)
    }
}

@Composable
private fun HorizontalRule(color: Color) {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(color))
}

@Composable
private fun RequestStatusRow(status: String, uiState: RequestUiState) {
    val colors = SubTideTheme.colors
    val derived = SubTideTheme.derivedColors
    val type = SubTideTheme.typography

    val (label, color) = when (status) {
        "pending", "in_progress", "in-progress" -> "PENDING…" to colors.muted
        "resolved" -> "RESOLVED" to colors.accent
        "rejected" -> "REJECTED" to derived.destructive
        "failed" -> "FAILED" to derived.destructive
        "unknown" -> "UNKNOWN" to derived.destructive
        else -> status.uppercase() to colors.muted
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = type.eyebrow, color = color)
        uiState.queuePosition?.let {
            Text(
                text = " · QUEUE #$it",
                style = type.monoLabel,
                color = colors.muted,
            )
        }
    }
    val track = uiState.resolvedTrack
    if (status == "resolved" && track != null) {
        Text(
            text = track.title ?: "—",
            style = type.bodyRegular,
            color = colors.ink,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(top = 6.dp),
        )
        val subtitle = listOfNotNull(track.artist, track.album).joinToString(" · ")
        if (subtitle.isNotEmpty()) {
            Text(text = subtitle, style = type.monoLabel, color = colors.muted)
        }
    }
}
