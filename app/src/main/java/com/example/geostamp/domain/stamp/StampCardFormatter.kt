package com.example.geostamp.domain.stamp

import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

class CardText(val title: String, val bodyLines: List<String>, val noteLine: String?)

/** Builds the words for the "map card" layout: a title, the address, coordinates, a long date and a note. */
object StampCardFormatter {

    fun format(content: StampContent, config: StampConfig, locale: Locale = Locale.getDefault()): CardText {
        val place = content.placeName?.trim()?.takeIf { it.isNotEmpty() }
        val address = if (content.locationSource != LocationSource.UNAVAILABLE) {
            content.address?.trim()?.takeIf { it.isNotEmpty() }
        } else {
            null
        }
        val title = place ?: AddressTitles.fromAddress(address) ?: "GeoStamp"

        val body = mutableListOf<String>()
        if (address != null) body += address
        if (StampFormatter.hasValidCoordinates(content)) {
            body += String.format(
                Locale.US, "Lat %.6f\u00B0 Long %.6f\u00B0", content.latitude!!, content.longitude!!
            )
        } else {
            body += "Location unavailable"
        }
        body += dateLine(content, config, locale)

        val note = if (config.showLabel && !content.label.isNullOrBlank()) "Note : " + content.label.trim() else null
        return CardText(title, body, note)
    }

    private fun dateLine(content: StampContent, config: StampConfig, locale: Locale): String {
        val zoned = content.capturedAt.atZone(content.zoneId)
        val text = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy hh:mm a", locale).format(zoned)
        if (!config.showTimeZone) return text
        val total = zoned.offset.totalSeconds
        val sign = if (total < 0) "-" else "+"
        val absolute = abs(total)
        return text + String.format(Locale.US, " GMT %s%02d:%02d", sign, absolute / 3600, (absolute % 3600) / 60)
    }
}

/** Makes a short place title such as "Sample City, Example State, India" from a full street address. */
object AddressTitles {
    fun fromAddress(address: String?): String? {
        if (address.isNullOrBlank()) return null
        val parts = address.split(',')
            .map { it.replace(Regex("\\d+"), "").trim() }
            .filter { it.isNotEmpty() }
        if (parts.isEmpty()) return null
        return parts.takeLast(3).joinToString(", ")
    }
}
