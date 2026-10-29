package com.example.geostamp.domain.stamp

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/** Predictable names for saved files: GeoStamp_20261013_140000_abcd1234.jpg */
object StampFileNames {
    private val FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.US)

    fun newId(): String = UUID.randomUUID().toString()

    fun forPhoto(capturedAt: Instant, zone: ZoneId, id: String, extension: String = "jpg"): String {
        val stamp = FORMAT.format(capturedAt.atZone(zone))
        val shortId = id.filter { it.isLetterOrDigit() }.take(8).ifEmpty { "photo" }
        return "GeoStamp_${stamp}_$shortId.$extension"
    }
}
