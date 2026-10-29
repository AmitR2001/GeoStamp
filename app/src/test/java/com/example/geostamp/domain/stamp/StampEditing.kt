// geostamp-iteration: 7
package com.example.geostamp.domain.stamp

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/** What the person has typed into the editor, as plain text. */
data class StampFields(
    val label: String = "",
    val address: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val date: String = "",
    val time: String = "",
    val zone: String = "",
    val placeName: String = ""
)

data class FieldErrors(
    val latitude: String? = null,
    val longitude: String? = null,
    val date: String? = null,
    val time: String? = null,
    val zone: String? = null
) {
    val hasErrors: Boolean
        get() = latitude != null || longitude != null || date != null || time != null || zone != null
}

data class EditResult(val content: StampContent, val errors: FieldErrors)

/**
 * Turns typed text into StampContent. Pure Kotlin, fully unit tested.
 *
 * Rule for honesty: location data stays "GPS" only while address and coordinates are exactly
 * what the GPS fix produced. Any change makes it "manual". Nothing is ever invented.
 */
object StampEditing {
    private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US)
    private const val SAME_COORDINATE_TOLERANCE = 0.000001

    fun formatCoordinate(value: Double): String = String.format(Locale.US, "%.6f", value)

    fun fieldsFrom(content: StampContent): StampFields {
        val zoned = content.capturedAt.atZone(content.zoneId)
        return StampFields(
            label = content.label.orEmpty(),
            address = content.address.orEmpty(),
            latitude = content.latitude?.let { formatCoordinate(it) }.orEmpty(),
            longitude = content.longitude?.let { formatCoordinate(it) }.orEmpty(),
            date = zoned.toLocalDate().toString(),
            time = TIME_FORMAT.format(zoned),
            zone = content.zoneId.id,
            placeName = content.placeName.orEmpty()
        )
    }

    fun parseLatitude(text: String): Double? =
        text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { StampFormatter.isValidLatitude(it) }

    fun parseLongitude(text: String): Double? =
        text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { StampFormatter.isValidLongitude(it) }

    fun parseZone(text: String): ZoneId? = runCatching { ZoneId.of(text.trim()) }.getOrNull()

    fun apply(fields: StampFields, original: StampContent): EditResult {
        var zoneError: String? = null
        var dateError: String? = null
        var timeError: String? = null
        var latError: String? = null
        var lonError: String? = null

        val parsedZone = parseZone(fields.zone)
        val zone: ZoneId
        if (parsedZone == null) {
            zoneError = "Use a zone like Asia/Kolkata, UTC or +05:30"
            zone = original.zoneId
        } else {
            zone = parsedZone
        }

        val date = runCatching { LocalDate.parse(fields.date.trim()) }.getOrNull()
        if (date == null) dateError = "Use the format yyyy-MM-dd, for example 2026-10-13"
        val time = runCatching { LocalTime.parse(fields.time.trim()) }.getOrNull()
        if (time == null) timeError = "Use the format HH:mm:ss, for example 14:30:00"

        val instant = if (date != null && time != null) {
            LocalDateTime.of(date, time).atZone(zone).toInstant()
        } else {
            original.capturedAt
        }

        val latText = fields.latitude.trim()
        val lonText = fields.longitude.trim()
        var lat: Double? = null
        var lon: Double? = null
        if (latText.isNotEmpty()) {
            lat = parseLatitude(latText)
            if (lat == null) latError = "Latitude must be a number from -90 to 90"
        }
        if (lonText.isNotEmpty()) {
            lon = parseLongitude(lonText)
            if (lon == null) lonError = "Longitude must be a number from -180 to 180"
        }
        if (latText.isEmpty() != lonText.isEmpty()) {
            if (latText.isEmpty()) latError = "Enter both latitude and longitude, or leave both empty"
            else lonError = "Enter both latitude and longitude, or leave both empty"
        }
        val coordinatesOk = lat != null && lon != null
        val finalLat = if (coordinatesOk) lat else null
        val finalLon = if (coordinatesOk) lon else null

        val address = fields.address.trim().ifEmpty { null }
        val label = fields.label.trim().ifEmpty { null }
        val placeName = fields.placeName.trim().ifEmpty { null }

        val originalAddress = original.address?.trim()?.ifEmpty { null }
        val unchanged = address == originalAddress &&
            sameCoordinate(finalLat, original.latitude) &&
            sameCoordinate(finalLon, original.longitude)
        val source = when {
            unchanged -> original.locationSource
            finalLat == null && finalLon == null && address == null -> LocationSource.UNAVAILABLE
            else -> LocationSource.MANUAL
        }

        val content = StampContent(
            address = address,
            latitude = finalLat,
            longitude = finalLon,
            locationSource = source,
            capturedAt = instant,
            zoneId = zone,
            label = label,
            placeName = placeName
        )
        return EditResult(content, FieldErrors(latError, lonError, dateError, timeError, zoneError))
    }

    private fun sameCoordinate(a: Double?, b: Double?): Boolean = when {
        a == null && b == null -> true
        a == null || b == null -> false
        else -> abs(a - b) <= SAME_COORDINATE_TOLERANCE
    }
}
