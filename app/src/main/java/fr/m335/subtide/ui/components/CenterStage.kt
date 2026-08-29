package fr.m335.subtide.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import fr.m335.subtide.ui.theme.SubTideTheme
import kotlin.math.roundToInt

@Composable
fun CenterStage(
    title: String,
    artist: String?,
    album: String?,
    genre: String?,
    bpm: Double?,
    key: String?,
    moodLine: String?,
    llmTokens: Int?,
    coverUrl: String?,
    elapsedSec: Int?,
    durationSec: Int?,
    isLiked: Boolean,
    likeCount: Int,
    onLikeClick: () -> Unit,
    onCoverClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SubTideTheme.colors
    val derived = SubTideTheme.derivedColors
    val type = SubTideTheme.typography

    val artistAlbumLine = listOfNotNull(artist, album).takeIf { it.isNotEmpty() }?.joinToString(" · ")
    val metadataLine = listOfNotNull(genre?.uppercase(), bpm?.let { "${it.roundToInt()} BPM" }, key?.uppercase())
        .takeIf { it.isNotEmpty() }
        ?.joinToString(" · ")
    val nowPlayingLabel = if (elapsedSec != null && durationSec != null) {
        "NOW PLAYING — ${formatClock(elapsedSec)} / ${formatClock(durationSec)}"
    } else {
        "NOW PLAYING"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CoverArt(coverUrl = coverUrl, onClick = onCoverClick)
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = nowPlayingLabel,
                style = type.monoLabel,
                color = colors.muted,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            llmTokens?.let {
                Text(text = "$it", style = type.monoLabel, color = derived.inkFaint)
                Spacer(Modifier.width(8.dp))
            }
            HeartIcon(
                isLiked = isLiked,
                modifier = Modifier
                    .size(18.dp)
                    .clickable(onClick = onLikeClick),
            )
            if (likeCount > 0) {
                Spacer(Modifier.width(3.dp))
                Text(
                    text = "$likeCount",
                    style = type.monoLabel.copy(fontSize = 10.sp),
                    color = derived.destructive,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = title,
            style = type.displayTitle,
            color = colors.ink,
            textAlign = TextAlign.Center,
        )
        artistAlbumLine?.let {
            Spacer(Modifier.height(4.dp))
            Text(text = it, style = type.bodyMedium, color = colors.muted, textAlign = TextAlign.Center)
        }
        metadataLine?.let {
            Spacer(Modifier.height(10.dp))
            Text(text = it, style = type.monoLabel, color = colors.muted)
        }
        moodLine?.let { mood ->
            Text(
                text = "↳ ${mood.uppercase()}",
                style = type.monoLabel,
                color = colors.accent,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
fun DjThinkingTicker(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    val colors = SubTideTheme.colors
    val derived = SubTideTheme.derivedColors
    val type = SubTideTheme.typography
    Text(
        text = text,
        style = type.bodyRegular.copy(fontSize = 13.sp),
        color = if (isError) derived.destructive else derived.inkFaint,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable(onClick = onClick),
    )
}

/** Hand-drawn to match the app's other custom-drawn controls — no Material icon set. */
@Composable
private fun HeartIcon(isLiked: Boolean, modifier: Modifier = Modifier) {
    val colors = SubTideTheme.colors
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.88f)
            cubicTo(w * -0.05f, h * 0.55f, w * 0.05f, h * 0.05f, w * 0.5f, h * 0.32f)
            cubicTo(w * 0.95f, h * 0.05f, w * 1.05f, h * 0.55f, w * 0.5f, h * 0.88f)
            close()
        }
        if (isLiked) {
            drawPath(path, color = colors.accent)
        } else {
            drawPath(path, color = colors.muted, style = Stroke(width = 1.5.dp.toPx()))
        }
    }
}

@Composable
private fun CoverArt(coverUrl: String?, onClick: () -> Unit) {
    val colors = SubTideTheme.colors
    Box(
        modifier = Modifier
            .size(120.dp)
            .background(colors.field)
            .border(BorderStroke(1.dp, colors.ink))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Canvas(modifier = Modifier.size(120.dp)) {
                val step = 14.dp.toPx()
                var x = -size.height
                while (x < size.width) {
                    drawLine(
                        color = colors.muted.copy(alpha = 0.35f),
                        start = Offset(x, size.height),
                        end = Offset(x + size.height, 0f),
                        strokeWidth = 1.dp.toPx(),
                    )
                    x += step
                }
            }
        }
    }
}

private fun formatClock(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%02d:%02d".format(m, s)
}
