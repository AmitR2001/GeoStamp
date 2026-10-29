package com.example.geostamp.data.media

import android.os.ParcelFileDescriptor
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream

/**
 * Copies the ORIGINAL camera metadata (date taken, camera model, exposure, ...) into the saved
 * copy. Nothing is invented and no location is added here. Orientation is set to "normal"
 * because the saved pixels are already rotated upright.
 */
object ExifCopier {
    private val TAGS = listOf(
        "DateTime", "DateTimeOriginal", "DateTimeDigitized", "OffsetTime", "OffsetTimeOriginal",
        "Make", "Model", "LensMake", "LensModel",
        "ExposureTime", "FNumber", "PhotographicSensitivity", "FocalLength", "Flash", "WhiteBalance",
        "GPSLatitude", "GPSLatitudeRef", "GPSLongitude", "GPSLongitudeRef",
        "GPSAltitude", "GPSAltitudeRef", "GPSTimeStamp", "GPSDateStamp"
    )

    /** Returns true when the copy worked. Failure is not fatal: the photo is saved either way. */
    fun copy(source: InputStream, destination: ParcelFileDescriptor): Boolean = try {
        val from = ExifInterface(source)
        val to = ExifInterface(destination.fileDescriptor)
        for (tag in TAGS) {
            from.getAttribute(tag)?.let { to.setAttribute(tag, it) }
        }
        to.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
        to.saveAttributes()
        true
    } catch (e: Exception) {
        false
    }
}
