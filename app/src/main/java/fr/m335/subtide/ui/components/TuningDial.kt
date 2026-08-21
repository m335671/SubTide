package fr.m335.subtide.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import fr.m335.subtide.ui.theme.SubTideTheme

/**
 * A radio-dial-style page indicator: dense hairline ticks with a sliding vermillion pointer over
 * the current page's label — replaces a generic swipe-dot indicator with something that reads as
 * "tuning a frequency" rather than a modern app tab bar.
 */
@Composable
fun TuningDial(pagerState: PagerState, labels: List<String>, modifier: Modifier = Modifier) {
    val colors = SubTideTheme.colors
    val derived = SubTideTheme.derivedColors
    val type = SubTideTheme.typography
    val pageFraction = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
        .coerceIn(0f, (labels.size - 1).toFloat())

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(18.dp)) {
            val minorTickCount = 48
            for (i in 0..minorTickCount) {
                val x = size.width * i / minorTickCount
                drawLine(
                    color = derived.line,
                    start = Offset(x, size.height * 0.55f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            val pointerX = if (labels.size > 1) size.width * pageFraction / (labels.size - 1) else size.width / 2f
            drawLine(
                color = colors.accent,
                start = Offset(pointerX, 0f),
                end = Offset(pointerX, size.height),
                strokeWidth = 2.dp.toPx(),
            )
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEachIndexed { index, label ->
                val isCurrent = index == pagerState.currentPage
                Text(
                    text = label,
                    style = type.eyebrow,
                    color = if (isCurrent) colors.accent else colors.muted,
                )
            }
        }
    }
}
