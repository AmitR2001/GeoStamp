package com.example.geostamp.domain.metadata

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/** Writes dates the way cameras store them in EXIF ("2026:10:13 14:00:00" and "+05:30"). */
object ExifFormat {
    private val FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss", Locale.US)

    fun dateTime(instant: Instant, zone: ZoneId): String = FORMAT.format(instant.atZone(zone))

    fun offset(instant: Instant, zone: ZoneId): String {
        val seconds = zone.rules.getOffset(instant).totalSeconds
        val sign = if (seconds < 0) "-" else "+"
        val absolute = abs(seconds)
        return String.format(Locale.US, "%s%02d:%02d", sign, absolute / 3600, (absolute % 3600) / 60)
    }
}
