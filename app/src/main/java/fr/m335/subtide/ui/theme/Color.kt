package fr.m335.subtide.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * The 7 themable tokens SUB/WAVE's `/themes` endpoint serves. Every screen must be built on
 * top of these, never on hardcoded colors, so a server-provided theme can replace them wholesale.
 */
data class SubwaveColors(
    val bg: Color,
    val ink: Color,
    val muted: Color,
    val accent: Color,
    val overlay: Color,
    val softBorder: Color,
    val field: Color,
)

/** Tokens derived from [SubwaveColors] by color-mixing, mirroring the web client's `color-mix` rules. */
data class SubwaveDerivedColors(
    val surface: Color,
    val inkFaint: Color,
    val line: Color,
    val accentSoft: Color,
    val destructive: Color,
)

fun deriveSubwaveColors(colors: SubwaveColors): SubwaveDerivedColors = SubwaveDerivedColors(
    surface = lerp(colors.bg, colors.ink, 0.07f),
    inkFaint = lerp(colors.ink, colors.bg, 0.55f),
    line = lerp(colors.ink, colors.bg, 0.70f),
    accentSoft = lerp(colors.accent, colors.bg, 0.14f),
    destructive = Color(0xFFC5302A),
)

/** One selectable entry of the future `/themes` catalog — shape mirrors the server payload. */
data class SubwaveThemeOption(
    val id: String,
    val name: String,
    val description: String,
    val mode: String,
    val colors: SubwaveColors,
)

object SubwaveThemeDefaults {
    val classicLight = SubwaveThemeOption(
        id = "classic-light",
        name = "Classic Light",
        description = "Newsprint paper, black ink, vermillion accent.",
        mode = "light",
        colors = SubwaveColors(
            bg = Color(0xFFF3EFE6),
            ink = Color(0xFF161412),
            muted = Color(0xFF7A736A),
            accent = Color(0xFFD94B2A),
            overlay = Color(0x0D000000),
            softBorder = Color(0x14000000),
            field = Color(0xFFE9E4D8),
        ),
    )

    val midnight = SubwaveThemeOption(
        id = "midnight",
        name = "Midnight",
        description = "Same fascia, lights off.",
        mode = "dark",
        colors = SubwaveColors(
            bg = Color(0xFF100E0C),
            ink = Color(0xFFECE6DC),
            muted = Color(0xFFC1C0BD),
            accent = Color(0xFFD94B2A),
            overlay = Color(0x8C000000),
            softBorder = Color(0x1AFFFFFF),
            field = Color(0xFF1B1815),
        ),
    )

    val all = listOf(classicLight, midnight)
}
