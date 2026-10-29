package com.example.geostamp.domain.stamp

import java.time.Instant
import java.time.ZoneId

/** Where the location text on a stamp came from. Never mix these up. */
enum class LocationSource { GPS, MANUAL, UNAVAILABLE }

enum class StampAlignment { START, CENTER, END }

enum class DateStyle(val pattern: String) {
    DAY_MONTH_YEAR("dd MMM yyyy"),
    MONTH_DAY_YEAR("MMM dd, yyyy"),
    ISO("yyyy-MM-dd")
}

enum class TimeStyle(val pattern: String) {
    H24("HH:mm:ss"),
    H12("hh:mm:ss a")
}

/**
 * The facts that go on the stamp. This is what the user can edit before saving.
 * It is NOT the photo's EXIF data and editing it never touches EXIF.
 */
data class StampContent(
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationSource: LocationSource = LocationSource.UNAVAILABLE,
    val capturedAt: Instant,
    val zoneId: ZoneId = ZoneId.systemDefault(),
    val label: String? = null
)

/** How the stamp looks and which parts it shows. Colors are ARGB ints. */
data class StampConfig(
    /** Text size as a fraction of image width, so it looks the same on any resolution. */
    val textSizeFraction: Float = 0.032f,
    val foregroundColor: Int = 0xFFFFFFFF.toInt(),
    val backgroundColor: Int = 0xB3000000.toInt(),
    val alignment: StampAlignment = StampAlignment.START,
    /** Padding as a fraction of image width. */
    val paddingFraction: Float = 0.025f,
    val dateStyle: DateStyle = DateStyle.DAY_MONTH_YEAR,
    val timeStyle: TimeStyle = TimeStyle.H24,
    val showTimeZone: Boolean = true,
    val showLocationSourceTag: Boolean = true,
    val showLabel: Boolean = true
)

