package com.example.geostamp.domain.batch

import com.example.geostamp.domain.stamp.LocationSource
import com.example.geostamp.domain.stamp.StampContent
import com.example.geostamp.domain.stamp.StampEditing
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** What was typed on the "stamp many photos" page. */
data class BatchFields(
    val placeName: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val address: String = "",
    val label: String = "",
    val setDateTime: Boolean = false,
    val date: String = "",
    val time: String = "",
    val zone: String = ""
)

class BatchErrors(
    val latitude: String? = null,
    val longitude: String? = null,
    val date: String? = null,
    val time: String? = null,
    val zone: String? = null
) {
    val any: Boolean get() = latitude != null || longitude != null || date != null || time != null || zone != null
}

/** The checked changes to apply to every photo. Anything null is left as each photo already has it. */
data class BatchOptions(
    val placeName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val label: String? = null,
    val capturedAt: Instant? = null,
    val zone: ZoneId? = null
) {
    val hasLocation: Boolean get() = latitude != null && longitude != null
    val hasDate: Boolean get() = capturedAt != null && zone != null
    val isEmpty: Boolean get() = placeName == null && !hasLocation && label == null && !hasDate
}

class BatchValidation(val options: BatchOptions?, val errors: BatchErrors)

object BatchEditing {

    fun validate(fields: BatchFields): BatchValidation {
        var latError: String? = null
        var lonError: String? = null
        var dateError: String? = null
        var timeError: String? = null
        var zoneError: String? = null

        val latText = fields.latitude.trim()
        val lonText = fields.longitude.trim()
        var lat: Double? = null
        var lon: Double? = null
        if (latText.isNotEmpty()) {
            lat = StampEditing.parseLatitude(latText)
            if (lat == null) latError = "Latitude must be a number from -90 to 90"
        }
        if (lonText.isNotEmpty()) {
            lon = StampEditing.parseLongitude(lonText)
            if (lon == null) lonError = "Longitude must be a number from -180 to 180"
        }
        if (latText.isEmpty() != lonText.isEmpty()) {
            if (latText.isEmpty()) latError = "Enter both latitude and longitude, or leave both empty"
            else lonError = "Enter both latitude and longitude, or leave both empty"
        }

        var instant: Instant? = null
        var zone: ZoneId? = null
        if (fields.setDateTime) {
            val date = runCatching { LocalDate.parse(fields.date.trim()) }.getOrNull()
            if (date == null) dateError = "Use the format yyyy-MM-dd"
            val time = runCatching { LocalTime.parse(fields.time.trim()) }.getOrNull()
            if (time == null) timeError = "Use the format HH:mm:ss"
            zone = StampEditing.parseZone(fields.zone)
            if (zone == null) zoneError = "Use a zone like Asia/Kolkata or +05:30"
            if (date != null && time != null && zone != null) {
                instant = LocalDateTime.of(date, time).atZone(zone).toInstant()
            }
        }

        val errors = BatchErrors(latError, lonError, dateError, timeError, zoneError)
        if (errors.any) return BatchValidation(null, errors)

        val options = BatchOptions(
            placeName = fields.placeName.trim().ifEmpty { null },
            latitude = lat,
            longitude = lon,
            address = fields.address.trim().ifEmpty { null },
            label = fields.label.trim().ifEmpty { null },
            capturedAt = instant,
            zone = if (instant != null) zone else null
        )
        return BatchValidation(options, errors)
    }

    /**
     * Applies the options to one photo's stamp. A new location replaces the old coordinates AND the old
     * address together, so an old address never sits next to new coordinates. Each photo keeps its own
     * date and time unless a date was set for all of them.
     */
    fun apply(base: StampContent, options: BatchOptions): StampContent {
        var content = base
        val lat = options.latitude
        val lon = options.longitude
        if (lat != null && lon != null) {
            content = content.copy(
                latitude = lat,
                longitude = lon,
                address = options.address,
                locationSource = LocationSource.MANUAL
            )
        }
        if (options.placeName != null) content = content.copy(placeName = options.placeName)
        if (options.label != null) content = content.copy(label = options.label)
        val at = options.capturedAt
        val zone = options.zone
        if (at != null && zone != null) content = content.copy(capturedAt = at, zoneId = zone)
        return content
    }
}
