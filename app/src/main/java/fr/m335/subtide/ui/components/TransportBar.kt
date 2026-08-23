package fr.m335.subtide.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.m335.subtide.ui.theme.SubTideTheme
import kotlin.math.cos
import kotlin.math.sin

/** The player's "console": Power | Signal meter | Volume, one bordered box, no floating cards. */
@Composable
fun TransportBar(
    isOnAir: Boolean,
    listeners: Int,
    latencyMs: Int?,
    isBuffering: Boolean,
    volume: Float,
    isMuted: Boolean,
    onPowerClick: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onMuteToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val derived = SubTideTheme.derivedColors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF211F1D))
            .border(1.dp, derived.line)
            .height(80.dp),
    ) {
        PowerCell(isOnAir = isOnAir, onClick = onPowerClick, modifier = Modifier.width(72.dp))
        VerticalDivider()
        SignalMeterCell(
            listeners = listeners,
            latencyMs = latencyMs,
            isBuffering = isBuffering,
            modifier = Modifier.weight(1f),
        )
        VerticalDivider()
        VolumeCell(
            volume = volume,
            isMuted = isMuted,
            onVolumeChange = onVolumeChange,
            onMuteToggle = onMuteToggle,
            modifier = Modifier.width(96.dp),
        )
    }
}

@Composable
private fun VerticalDivider() {
    val derived = SubTideTheme.derivedColors
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(derived.line),
    )
}

@Composable
private fun PowerCell(isOnAir: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = SubTideTheme.colors
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickableNoRipple(onClick),
        contentAlignment = Alignment.Center,
    ) {
        val icon = painterResource(id = android.R.drawable.ic_lock_power_off)
        val ringColor = if (isOnAir) colors.accent else colors.muted

        Canvas(modifier = Modifier.size(42.dp)) { // adapte à ta taille existante
            val iconSize = size.minDimension  // équivalent de ton rayon réduit
            with(icon) {
                translate(
                    left = (size.width - iconSize) / 2f,
                    top = (size.height - iconSize) / 2f
                ) {
                    draw(
                        size = Size(iconSize, iconSize),
                        colorFilter = ColorFilter.tint(ringColor)
                    )
                }
            }
        }
    }
}

private val MAJOR_TICK_VALUES = listOf(0, 50, 100, 150, 200, 250)
private const val RULER_MAX_MS = 250f

/** "Signal · quality" label, listener count + measured latency, and a 0–250ms ruler with a marker. */
@Composable
private fun SignalMeterCell(
    listeners: Int,
    latencyMs: Int?,
    isBuffering: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = SubTideTheme.colors
    val derived = SubTideTheme.derivedColors
    val type = SubTideTheme.typography

    val (qualityLabel, qualityTargetColor) = when {
        isBuffering -> "BUFFERING" to colors.muted
        latencyMs == null -> "—" to colors.muted
        latencyMs < 100 -> "GOOD" to colors.accent
        latencyMs < 200 -> "FAIR" to colors.muted
        else -> "POOR" to derived.destructive
    }
    val qualityColor by animateColorAsState(targetValue = qualityTargetColor, label = "signalQualityColor")
    val infoLine = buildString {
        append(listeners)
        append(" ♪")
        latencyMs?.let { append(" · $it MS") }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "SIGNAL", style = type.eyebrow, color = colors.muted)
            Text(text = qualityLabel, style = type.eyebrow, color = qualityColor)
        }
        Spacer(Modifier.height(2.dp))
        Text(text = infoLine, style = type.monoLabel, color = colors.muted, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        SignalRuler(latencyMs = latencyMs, modifier = Modifier.fillMaxWidth().weight(1f))
    }
}

@Composable
private fun SignalRuler(latencyMs: Int?, modifier: Modifier = Modifier) {
    val colors = SubTideTheme.colors
    val derived = SubTideTheme.derivedColors
    val type = SubTideTheme.typography
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = type.monoLabel.copy(fontSize = 9.sp, color = colors.muted)

    val targetFraction = (latencyMs?.coerceIn(0, RULER_MAX_MS.toInt()) ?: 0) / RULER_MAX_MS
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 700),
        label = "signalMarkerPosition",
    )
    val markerAlpha by animateFloatAsState(
        targetValue = if (latencyMs != null) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "signalMarkerAlpha",
    )

    Canvas(modifier = modifier) {
        val rulerBottom = size.height * 0.55f
        val minorTickCount = 50
        for (i in 0..minorTickCount) {
            val x = size.width * i / minorTickCount
            val isMajor = i % (minorTickCount / (MAJOR_TICK_VALUES.size - 1)) == 0
            drawLine(
                color = derived.line,
                start = Offset(x, rulerBottom),
                end = Offset(x, rulerBottom - (if (isMajor) 10.dp.toPx() else 5.dp.toPx())),
                strokeWidth = 1.dp.toPx(),
            )
        }
        MAJOR_TICK_VALUES.forEach { value ->
            val x = size.width * (value / RULER_MAX_MS)
            val text = value.toString()
            val measured = textMeasurer.measure(text, labelStyle)
            drawText(
                textMeasurer = textMeasurer,
                text = text,
                style = labelStyle,
                topLeft = Offset(
                    (x - measured.size.width / 2f).coerceIn(0f, size.width - measured.size.width),
                    rulerBottom + 2.dp.toPx(),
                ),
            )
        }
        if (markerAlpha > 0f) {
            val markerX = size.width * animatedFraction
            drawLine(
                color = colors.accent.copy(alpha = markerAlpha),
                start = Offset(markerX, 0f),
                end = Offset(markerX, rulerBottom),
                strokeWidth = 2.dp.toPx(),
            )
            drawCircle(
                color = colors.accent.copy(alpha = markerAlpha),
                radius = 2.5.dp.toPx(),
                center = Offset(markerX, rulerBottom * 0.35f),
            )
        }
    }
}

@Composable
private fun VolumeCell(
    volume: Float,
    isMuted: Boolean,
    onVolumeChange: (Float) -> Unit,
    onMuteToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SubTideTheme.colors
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RotaryKnob(
            volume = volume,
            onVolumeChange = onVolumeChange,
            modifier = Modifier.size(44.dp),
        )
        Spacer(Modifier.height(8.dp))
        MuteDotGrid(isMuted = isMuted, onClick = onMuteToggle)
    }
}

/**
 * Vertical drag rotates the knob. `pointerInput(Unit)` installs the gesture detector once and never
 * restarts it, so its `onDrag` lambda would otherwise close over the `volume` value from whenever
 * the drag started rather than the live one — [rememberUpdatedState] keeps it current. Sensitivity
 * is a fixed drag distance rather than the knob's own (tiny) size, so it isn't twitchy.
 */
@Composable
private fun RotaryKnob(volume: Float, onVolumeChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    val colors = SubTideTheme.colors
    val derived = SubTideTheme.derivedColors
    val minAngle = -135f
    val maxAngle = 135f
    val currentVolume = rememberUpdatedState(volume)
    val fullRangePx = with(LocalDensity.current) { 160.dp.toPx() }

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                val delta = -dragAmount.y / fullRangePx
                onVolumeChange((currentVolume.value + delta).coerceIn(0f, 1f))
            }
        },
    ) {
        val strokeWidth = 2.dp.toPx()
        val radius = size.minDimension / 2f - strokeWidth
        drawCircle(color = derived.line, radius = radius, style = Stroke(width = strokeWidth))

        val tickCount = 11
        for (i in 0 until tickCount) {
            val t = i / (tickCount - 1f)
            val angleDeg = minAngle + (maxAngle - minAngle) * t
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val inner = radius - 4.dp.toPx()
            val outer = radius
            val start = Offset(
                center.x + inner * sin(angleRad).toFloat(),
                center.y - inner * cos(angleRad).toFloat(),
            )
            val end = Offset(
                center.x + outer * sin(angleRad).toFloat(),
                center.y - outer * cos(angleRad).toFloat(),
            )
            drawLine(color = derived.line, start = start, end = end, strokeWidth = 1.dp.toPx())
        }

        val pointerAngleDeg = minAngle + (maxAngle - minAngle) * volume.coerceIn(0f, 1f)
        val pointerAngleRad = Math.toRadians(pointerAngleDeg.toDouble())
        val pointerLength = radius - 6.dp.toPx()
        drawLine(
            color = colors.accent,
            start = center,
            end = Offset(
                center.x + pointerLength * sin(pointerAngleRad).toFloat(),
                center.y - pointerLength * cos(pointerAngleRad).toFloat(),
            ),
            strokeWidth = 2.dp.toPx(),
        )
    }
}

@Composable
private fun MuteDotGrid(isMuted: Boolean, onClick: () -> Unit) {
    val colors = SubTideTheme.colors
    val derived = SubTideTheme.derivedColors
    Row(
        modifier = Modifier.clickableNoRipple(onClick),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(
                        color = if (isMuted) colors.accent else derived.line,
                        shape = CircleShape,
                    ),
            )
        }
    }
}

/** Tap target with no ripple — the brutalist style has no Material elevation/ripple feedback. */
@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
}
