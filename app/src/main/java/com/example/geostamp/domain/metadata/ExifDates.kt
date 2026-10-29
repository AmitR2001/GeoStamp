package com.example.geostamp.domain.metadata

import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

class ExifTime(val instant: Instant, val zone: ZoneId)

/** Reads the date format cameras write into photos ("2026:10:13 14:00:00"). */
object ExifDates {
    private val FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss", Locale.US)

    /**
     * [dateText] is the EXIF date, [offsetText] the optional EXIF offset like "+05:30".
     * Without a valid offset the [fallbackZone] is assumed. Returns null for missing or nonsense dates.
     */
    fun parse(dateText: String?, offsetText: String?, fallbackZone: ZoneId): ExifTime? {
        if (dateText.isNullOrBlank()) return null
        val local = try {
            LocalDateTime.parse(dateText.trim(), FORMAT)
        } catch (e: DateTimeException) {
            return null
        }
        if (local.year < 1970) return null
        val zone: ZoneId = offsetText?.trim()?.takeIf { it.isNotEmpty() }?.let {
            try {
                ZoneOffset.of(it)
            } catch (e: DateTimeException) {
                null
            }
        } ?: fallbackZone
        return ExifTime(local.atZone(zone).toInstant(), zone)
    }
}
