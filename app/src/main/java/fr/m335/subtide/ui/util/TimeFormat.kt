package fr.m335.subtide.ui.util

import java.time.Instant
import java.time.ZoneId

/**
 * Formats an ISO-8601 instant as `HH:mm` in the station's timezone rather than the device's — per
 * CLAUDE_CODE_PROMPT.md, timestamps should read in the station's local time (e.g. for the Booth
 * transcript), using the `timezone` field from `/state`/`/schedule`.
 */
fun formatStationTime(iso: String?, timezone: String?): String {
    if (iso.isNullOrBlank()) return "--:--"
    return try {
        val zoneId = timezone?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
        val time = Instant.parse(iso).atZone(zoneId).toLocalTime()
        "%02d:%02d".format(time.hour, time.minute)
    } catch (e: Exception) {
        "--:--"
    }
}
