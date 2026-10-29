package com.example.geostamp.domain.location

/** Result of asking Android for the device location. Never guessed, never invented. */
sealed interface LocationResult {
    data class Fix(
        val latitude: Double,
        val longitude: Double,
        val accuracyMeters: Float?,
        val approximate: Boolean,
        val fixTimeMillis: Long
    ) : LocationResult

    data class Unavailable(val reason: Reason) : LocationResult

    enum class Reason { PERMISSION_DENIED, LOCATION_OFF, NO_FIX, ERROR }
}
