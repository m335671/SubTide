package fr.m335.subtide.data

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/**
 * Parses the color notations `/themes` actually sends: hex, `rgb()`/`rgba()`, and `oklch()` (used
 * directly as several bundled themes' `--accent`, e.g. `oklch(0.62 0.22 25)` — the vermillion from
 * design-system.md). `color-mix(...)` tokens (used for derived fields like `--field`) are
 * deliberately NOT parsed here — design-system.md says to just lerp from the base tokens instead of
 * replicating `color-mix` exactly, so callers should fall back to [deriveSubwaveColors]-style lerp
 * when this returns null.
 */
fun parseCssColor(raw: String?): Color? {
    val value = raw?.trim() ?: return null
    return when {
        value.startsWith("#") -> parseHex(value)
        value.startsWith("rgba(", ignoreCase = true) || value.startsWith("rgb(", ignoreCase = true) ->
            parseRgb(value)
        value.startsWith("oklch(", ignoreCase = true) -> parseOklch(value)
        else -> null
    }
}

private fun parseHex(hex: String): Color? {
    val h = hex.removePrefix("#")
    return try {
        when (h.length) {
            3 -> Color(
                red = ("${h[0]}${h[0]}").toInt(16) / 255f,
                green = ("${h[1]}${h[1]}").toInt(16) / 255f,
                blue = ("${h[2]}${h[2]}").toInt(16) / 255f,
            )
            6 -> Color(("FF$h").toLong(16).toInt())
            8 -> {
                val argb = "${h.substring(6, 8)}${h.substring(0, 6)}"
                Color(argb.toLong(16).toInt())
            }
            else -> null
        }
    } catch (e: NumberFormatException) {
        null
    }
}

private fun parseRgb(value: String): Color? {
    val inside = value.substringAfter('(').substringBefore(')')
    val parts = inside.split(',', '/').map { it.trim() }.filter { it.isNotEmpty() }
    if (parts.size < 3) return null
    return try {
        val r = parts[0].removeSuffix("%").trim().toFloat().let { if (parts[0].endsWith("%")) it * 2.55f else it }
        val g = parts[1].removeSuffix("%").trim().toFloat().let { if (parts[1].endsWith("%")) it * 2.55f else it }
        val b = parts[2].removeSuffix("%").trim().toFloat().let { if (parts[2].endsWith("%")) it * 2.55f else it }
        val a = if (parts.size > 3) parts[3].removeSuffix("%").trim().toFloat().let {
            if (parts[3].endsWith("%")) it / 100f else it
        } else 1f
        Color(red = r / 255f, green = g / 255f, blue = b / 255f, alpha = a)
    } catch (e: NumberFormatException) {
        null
    }
}

/** `oklch(L C H)` — L in [0,1] (or a percentage), C the chroma, H the hue in degrees. */
private fun parseOklch(value: String): Color? {
    val inside = value.substringAfter('(').substringBefore(')')
    val (colorPart, alphaPart) = if ('/' in inside) {
        inside.substringBefore('/').trim() to inside.substringAfter('/').trim()
    } else {
        inside.trim() to null
    }
    val parts = colorPart.split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (parts.size < 3) return null
    return try {
        val l = parts[0].removeSuffix("%").toDouble().let { if (parts[0].endsWith("%")) it / 100.0 else it }
        val c = parts[1].toDouble()
        val hDeg = parts[2].removeSuffix("deg").toDouble()
        val alpha = alphaPart?.removeSuffix("%")?.toFloat()?.let { if (alphaPart.endsWith("%")) it / 100f else it } ?: 1f
        oklchToColor(l, c, hDeg, alpha)
    } catch (e: NumberFormatException) {
        null
    }
}

private fun oklchToColor(l: Double, c: Double, hueDegrees: Double, alpha: Float): Color {
    val hueRad = Math.toRadians(hueDegrees)
    val a = c * kotlin.math.cos(hueRad)
    val b = c * kotlin.math.sin(hueRad)

    val l_ = l + 0.3963377774 * a + 0.2158037573 * b
    val m_ = l - 0.1055613458 * a - 0.0638541728 * b
    val s_ = l - 0.0894841775 * a - 1.2914855480 * b

    val lCubed = l_ * l_ * l_
    val mCubed = m_ * m_ * m_
    val sCubed = s_ * s_ * s_

    val rLinear = 4.0767416621 * lCubed - 3.3077115913 * mCubed + 0.2309699292 * sCubed
    val gLinear = -1.2684380046 * lCubed + 2.6097574011 * mCubed - 0.3413193965 * sCubed
    val bLinear = -0.0041960863 * lCubed - 0.7034186147 * mCubed + 1.7076147010 * sCubed

    return Color(
        red = linearToSrgb(rLinear),
        green = linearToSrgb(gLinear),
        blue = linearToSrgb(bLinear),
        alpha = alpha,
    )
}

private fun linearToSrgb(value: Double): Float {
    val clamped = value.coerceIn(0.0, 1.0)
    val encoded = if (clamped <= 0.0031308) 12.92 * clamped else 1.055 * clamped.pow(1.0 / 2.4) - 0.055
    return encoded.toFloat().coerceIn(0f, 1f)
}
