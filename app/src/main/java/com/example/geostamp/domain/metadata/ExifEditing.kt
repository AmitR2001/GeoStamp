package com.example.geostamp.domain.metadata

import com.example.geostamp.domain.stamp.StampEditing
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ExifEditErrors(
    val latitude: String? = null,
    val longitude: String? = null,
    val date: String? = null,
    val time: String? = null,
    val zone: String? = null
) {
    val any: Boolean get() = latitude != null || longitude != null || date != null || time != null || zone != null
}

class ExifEditResult(val changes: ExifChanges?, val errors: ExifEditErrors)

/** Checks what was typed into the metadata editor. Nothing is written unless it is valid and was switched on. */
object ExifEditing {
    fun validate(
        changeLocation: Boolean,
        latitude: String,
        longitude: String,
        changeDate: Boolean,
        date: String,
        time: String,
        zone: String
    ): ExifEditResult {
        var latError: String? = null
        var lonError: String? = null
        var dateError: String? = null
        var timeError: String? = null
        var zoneError: String? = null
        var lat: Double? = null
        var lon: Double? = null
        var instant: java.time.Instant? = null
        var zoneId: java.time.ZoneId? = null

        if (changeLocation) {
            lat = StampEditing.parseLatitude(latitude)
            if (lat == null) latError = "Latitude must be a number from -90 to 90"
            lon = StampEditing.parseLongitude(longitude)
            if (lon == null) lonError = "Longitude must be a number from -180 to 180"
        }
        if (changeDate) {
            val parsedDate = runCatching { LocalDate.parse(date.trim()) }.getOrNull()
            if (parsedDate == null) dateError = "Use the format yyyy-MM-dd"
            val parsedTime = runCatching { LocalTime.parse(time.trim()) }.getOrNull()
            if (parsedTime == null) timeError = "Use the format HH:mm:ss"
            zoneId = StampEditing.parseZone(zone)
            if (zoneId == null) zoneError = "Use a zone like Asia/Kolkata or +05:30"
            if (parsedDate != null && parsedTime != null && zoneId != null) {
                instant = LocalDateTime.of(parsedDate, parsedTime).atZone(zoneId).toInstant()
            }
        }

        val errors = ExifEditErrors(latError, lonError, dateError, timeError, zoneError)
        if (errors.any || (!changeLocation && !changeDate)) return ExifEditResult(null, errors)
        return ExifEditResult(
            ExifChanges(
                latitude = if (changeLocation) lat else null,
                longitude = if (changeLocation) lon else null,
                capturedAt = if (changeDate) instant else null,
                zone = if (changeDate) zoneId else null
            ),
            errors
        )
    }
}
