package fr.m335.subtide.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** One TextStyle per role from `design-system.md` §3 — never mix a family outside its role. */
data class SubwaveTypography(
    /** CenterStage track title — Fraunces SemiBold ~26sp/30sp. */
    val displayTitle: TextStyle,
    /** Same family, lighter weight — "scanning the dial_" placeholder and similar. */
    val displayLight: TextStyle,
    /** Artist / album / year line — Plus Jakarta Sans Medium ~15sp. */
    val bodyMedium: TextStyle,
    /** General running text, buttons, list rows — Plus Jakarta Sans Regular ~15sp. */
    val bodyRegular: TextStyle,
    /** Technical metadata (genre, BPM, key, timestamps) — mono ~11sp, uppercase, tracked. */
    val monoLabel: TextStyle,
    /** Section eyebrows ("NOW PLAYING", station name) — mono, uppercase, 3sp tracking. */
    val eyebrow: TextStyle,
)

val subwaveTypography = SubwaveTypography(
    displayTitle = TextStyle(
        fontFamily = displayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 30.sp,
    ),
    displayLight = TextStyle(
        fontFamily = displayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
        lineHeight = 30.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = bodyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
    ),
    bodyRegular = TextStyle(
        fontFamily = bodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
    ),
    monoLabel = TextStyle(
        fontFamily = monoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        letterSpacing = 1.5.sp,
    ),
    eyebrow = TextStyle(
        fontFamily = monoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 3.sp,
    ),
)
