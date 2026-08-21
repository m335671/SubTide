package fr.m335.subtide.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import fr.m335.subtide.ui.theme.SubwaveColors
import fr.m335.subtide.ui.theme.SubwaveThemeOption

/**
 * Maps a raw [ThemeDto] from `/themes` into a [SubwaveThemeOption]. `bg`/`ink` are required — a
 * theme missing either is skipped rather than guessed. The rest fall back to a lerp derived from
 * whatever base tokens *did* parse, matching design-system.md's "just lerp, don't replicate
 * `color-mix`" guidance for tokens that arrive as CSS functions [parseCssColor] can't resolve.
 */
fun ThemeDto.toThemeOption(): SubwaveThemeOption? {
    val themeId = id ?: return null
    val bg = parseCssColor(tokens?.bg) ?: return null
    val ink = parseCssColor(tokens?.ink) ?: return null
    val muted = parseCssColor(tokens?.muted) ?: lerp(ink, bg, 0.45f)
    val accent = parseCssColor(tokens?.accent) ?: Color(0xFFD94B2A)
    val overlay = parseCssColor(tokens?.overlay) ?: Color(0x0D000000)
    val softBorder = parseCssColor(tokens?.softBorder) ?: Color(0x14000000)
    val field = parseCssColor(tokens?.field) ?: lerp(bg, ink, 0.08f)

    return SubwaveThemeOption(
        id = themeId,
        name = name ?: themeId,
        description = description ?: "",
        mode = mode ?: "dark",
        colors = SubwaveColors(
            bg = bg,
            ink = ink,
            muted = muted,
            accent = accent,
            overlay = overlay,
            softBorder = softBorder,
            field = field,
        ),
    )
}

fun ThemesResponse.toThemeOptions(): List<SubwaveThemeOption> = themes.orEmpty().mapNotNull { it.toThemeOption() }
