package com.example.geostamp.permissions

/** What the user has allowed for location. Android lets people grant "approximate" only. */
enum class LocationAccess {
    NONE,
    APPROXIMATE,
    PRECISE;

    companion object {
        fun from(fineGranted: Boolean, coarseGranted: Boolean): LocationAccess = when {
            fineGranted -> PRECISE
            coarseGranted -> APPROXIMATE
            else -> NONE
        }
    }
}
