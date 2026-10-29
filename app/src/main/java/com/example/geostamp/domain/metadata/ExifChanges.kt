package com.example.geostamp.domain.metadata

import java.time.Instant
import java.time.ZoneId

/**
 * Changes the person explicitly asked to make to a photo's hidden metadata (EXIF).
 * Anything left null is left exactly as it is.
 */
class ExifChanges(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val removeLocation: Boolean = false,
    val capturedAt: Instant? = null,
    val zone: ZoneId? = null
) {
    val changesLocation: Boolean get() = removeLocation || (latitude != null && longitude != null)
    val changesDate: Boolean get() = capturedAt != null && zone != null
    val isEmpty: Boolean get() = !changesLocation && !changesDate
}
