// geostamp-iteration: 7
package com.example.geostamp.domain.stamp

import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/**
 * Turns StampContent + StampConfig into the plain text lines drawn on the photo (bar layout).
 * Pure Kotlin, no Android classes, so it is easy to unit test.
 * The stamp never says whether a location came from GPS or was typed; the app records that separately.
 */
object StampFormatter {

    fun format(
        content: StampContent,
        config: StampConfig,
        locale: Locale = Locale.getDefault()
    ): List<String> {
        val lines = mutableListOf<String>()

        if (config.showLabel && !content.label.isNullOrBlank()) {
            lines += content.label.trim()
        }
        val placeLine = placeAndAddress(content)
        if (placeLine.isNotEmpty()) lines += placeLine
        lines += coordinatesLine(content)
        lines += dateTimeLine(content, config, locale)
        return lines
    }

    /** "Example High School, Riverside Nagar, Sample City ..." (place name first, then the address). */
    fun placeAndAddress(content: StampContent): String {
        val place = content.placeName?.trim()?.takeIf { it.isNotEmpty() }
        val address = if (content.locationSource != LocationSource.UNAVAILABLE) {
            content.address?.trim()?.takeIf { it.isNotEmpty() }
        } else {
            null
        }
        return listOfNotNull(place, address).joinToString(", ")
    }

    fun hasValidCoordinates(content: StampContent): Boolean {
        val lat = content.latitude
        val lon = content.longitude
        return content.locationSource != LocationSource.UNAVAILABLE &&
            lat != null && lon != null && isValidLatitude(lat) && isValidLongitude(lon)
    }

    private fun coordinatesLine(content: StampContent): String {
        if (!hasValidCoordinates(content)) return "Location unavailable"
        return formatCoordinate(content.latitude!!, "N", "S") + ", " + formatCoordinate(content.longitude!!, "E", "W")
    }

    private fun dateTimeLine(content: StampContent, config: StampConfig, locale: Locale): String {
        val zoned = content.capturedAt.atZone(content.zoneId)
        val date = DateTimeFormatter.ofPattern(config.dateStyle.pattern, locale).format(zoned)
        val time = DateTimeFormatter.ofPattern(config.timeStyle.pattern, locale).format(zoned)
        val base = "$date $time"
        return if (config.showTimeZone) "$base ${offsetText(zoned.offset)}" else base
    }

    internal fun formatCoordinate(value: Double, positive: String, negative: String): String {
        val hemisphere = if (value >= 0) positive else negative
        return String.format(Locale.US, "%.5f° %s", abs(value), hemisphere)
    }

    internal fun offsetText(offset: ZoneOffset): String {
        val total = offset.totalSeconds
        val sign = if (total < 0) "-" else "+"
        val absTotal = abs(total)
        return String.format(Locale.US, "UTC%s%02d:%02d", sign, absTotal / 3600, (absTotal % 3600) / 60)
    }

    fun isValidLatitude(value: Double) = !value.isNaN() && value in -90.0..90.0
    fun isValidLongitude(value: Double) = !value.isNaN() && value in -180.0..180.0
}
