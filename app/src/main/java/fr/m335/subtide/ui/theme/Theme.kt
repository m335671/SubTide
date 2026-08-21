package fr.m335.subtide.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalSubwaveColors = staticCompositionLocalOf { SubwaveThemeDefaults.classicLight.colors }
private val LocalSubwaveDerivedColors = staticCompositionLocalOf {
    deriveSubwaveColors(SubwaveThemeDefaults.classicLight.colors)
}

/**
 * Composes the app on top of a [SubwaveThemeOption] rather than a fixed palette, so a theme fetched
 * from `GET /themes` can replace [themeOption] later without any call site changing. Material 3 is
 * used only as the technical component skeleton (ripple, text selection, focus) — its color scheme,
 * shapes and defaults are entirely overridden by the SUB/WAVE tokens below.
 */
@Composable
fun SubTideTheme(
    themeOption: SubwaveThemeOption = if (isSystemInDarkTheme()) {
        SubwaveThemeDefaults.midnight
    } else {
        SubwaveThemeDefaults.classicLight
    },
    content: @Composable () -> Unit,
) {
    val colors = themeOption.colors
    val derived = deriveSubwaveColors(colors)
    val materialColorScheme = if (themeOption.mode == "dark") {
        darkColorScheme(
            background = colors.bg,
            onBackground = colors.ink,
            surface = derived.surface,
            onSurface = colors.ink,
            surfaceVariant = colors.field,
            onSurfaceVariant = colors.muted,
            primary = colors.accent,
            onPrimary = colors.bg,
            secondary = colors.accent,
            outline = derived.line,
            error = derived.destructive,
        )
    } else {
        lightColorScheme(
            background = colors.bg,
            onBackground = colors.ink,
            surface = derived.surface,
            onSurface = colors.ink,
            surfaceVariant = colors.field,
            onSurfaceVariant = colors.muted,
            primary = colors.accent,
            onPrimary = colors.bg,
            secondary = colors.accent,
            outline = derived.line,
            error = derived.destructive,
        )
    }

    CompositionLocalProvider(
        LocalSubwaveColors provides colors,
        LocalSubwaveDerivedColors provides derived,
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            shapes = SubwaveShapes,
            content = content,
        )
    }
}

/** Mirrors the `MaterialTheme` accessor pattern: [SubTideTheme] (the function) sets these up. */
object SubTideTheme {
    val colors: SubwaveColors
        @Composable get() = LocalSubwaveColors.current

    val derivedColors: SubwaveDerivedColors
        @Composable get() = LocalSubwaveDerivedColors.current

    val typography: SubwaveTypography
        get() = subwaveTypography
}
