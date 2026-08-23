package fr.m335.subtide.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.m335.subtide.ui.theme.SubTideTheme

@Composable
fun TopBar(
    stationName: String,
    showName: String?,
    djName: String?,
    tagline: String,
    isTunedIn: Boolean,
    hasActiveIndicator: Boolean,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SubTideTheme.colors
    val derived = SubTideTheme.derivedColors
    val type = SubTideTheme.typography

    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = derived.line,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            DiscMark(isTunedIn = isTunedIn)
            Spacer(Modifier.width(10.dp))
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(text = stationName.uppercase(), style = type.eyebrow, color = colors.ink)
                if (showName != null || djName != null) {
                    Spacer(Modifier.width(1.dp))
                    showName?.let {
                        Text(
                            text = "▸ ${it.uppercase()}",
                            style = type.monoLabel,
                            color = colors.muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    djName?.let {
                        Text(
                            text = "WITH ${it.uppercase()}",
                            style = type.monoLabel,
                            color = colors.accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            SettingsButton(hasActiveIndicator = hasActiveIndicator, onClick = onSettingsClick)
        }
        Text(
            text = tagline,
            style = type.monoLabel,
            color = derived.inkFaint,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun DiscMark(isTunedIn: Boolean) {
    val colors = SubTideTheme.colors
    val transition = rememberInfiniteTransition(label = "disc-rotation")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "disc-angle",
    )

    Canvas(modifier = Modifier.size(22.dp)) {
        rotate(angle) {
            drawCircle(color = colors.ink, style = Stroke(width = 1.5.dp.toPx()))
            drawLine(
                color = if (isTunedIn) colors.accent else colors.muted,
                start = center,
                end = Offset(center.x, 0f),
                strokeWidth = 1.5.dp.toPx(),
            )
        }
    }
}

@Composable
private fun RowScope.SettingsButton(hasActiveIndicator: Boolean, onClick: () -> Unit) {
    val colors = SubTideTheme.colors
    Box(
        modifier = Modifier
            .size(32.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(18.dp)) {
            val step = size.width / 3f
            for (i in 0 until 3) {
                val x = step * i + step / 2f
                drawLine(
                    color = colors.ink,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
                val notchY = size.height * (0.3f + 0.2f * i)
                drawCircle(color = colors.ink, radius = 2.dp.toPx(), center = Offset(x, notchY))
            }
        }
        if (hasActiveIndicator) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(6.dp)
                    .background(color = colors.accent, shape = CircleShape),
            )
        }
    }
}
