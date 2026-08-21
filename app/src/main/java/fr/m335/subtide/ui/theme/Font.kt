package fr.m335.subtide.ui.theme

import androidx.compose.ui.text.font.FontFamily

/**
 * Stand-ins for the design system's three families (Fraunces / Plus Jakarta Sans / JetBrains Mono).
 * No bundled font files ship in the repo yet, so each role falls back to the closest system generic
 * family. Swap these for the real `res/font` resources without touching any call site — every text
 * style in [SubwaveTypography] is sourced from here.
 */
val displayFontFamily: FontFamily = FontFamily.Serif
val bodyFontFamily: FontFamily = FontFamily.SansSerif
val monoFontFamily: FontFamily = FontFamily.Monospace
