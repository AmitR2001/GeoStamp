package com.example.geostamp.data.media

import android.os.ParcelFileDescriptor
import androidx.exifinterface.media.ExifInterface
import com.example.geostamp.domain.metadata.ExifChanges
import com.example.geostamp.domain.metadata.ExifFormat

/** Writes ONLY the changes the person asked for into an EXIF block. Everything else stays as it was. */
object ExifWriter {
    private val GPS_TAGS = listOf(
        "GPSLatitude", "GPSLatitudeRef", "GPSLongitude", "GPSLongitudeRef",
        "GPSAltitude", "GPSAltitudeRef", "GPSTimeStamp", "GPSDateStamp", "GPSProcessingMethod"
    )

    fun apply(destination: ParcelFileDescriptor, changes: ExifChanges) {
        val exif = ExifInterface(destination.fileDescriptor)
        if (changes.removeLocation) {
            for (tag in GPS_TAGS) exif.setAttribute(tag, null)
        } else {
            val lat = changes.latitude
            val lon = changes.longitude
            if (lat != null && lon != null) exif.setLatLong(lat, lon)
        }
        val instant = changes.capturedAt
        val zone = changes.zone
        if (instant != null && zone != null) {
            exif.setAttribute("DateTimeOriginal", ExifFormat.dateTime(instant, zone))
            exif.setAttribute("OffsetTimeOriginal", ExifFormat.offset(instant, zone))
        }
        exif.saveAttributes()
    }
}
