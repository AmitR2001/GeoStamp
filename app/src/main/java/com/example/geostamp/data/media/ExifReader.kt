package com.example.geostamp.data.media

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.example.geostamp.domain.metadata.ExifEntry
import com.example.geostamp.domain.metadata.ExifSummary
import java.io.IOException

class ExifSnapshot(val entries: List<ExifEntry>, val latitude: Double?, val longitude: Double?, val dateTimeOriginal: String?)

/** Reads a photo's hidden metadata (EXIF) without changing anything. Call from a background thread. */
class ExifReader(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    fun read(uri: Uri): ExifSnapshot {
        val stream = resolver.openInputStream(uri) ?: throw IOException("Could not open the photo.")
        return stream.use { input ->
            val exif = ExifInterface(input)
            val raw = ExifSummary.TAGS.associateWith { exif.getAttribute(it) }
            val latLong = exif.latLong
            val lat = latLong?.get(0)
            val lon = latLong?.get(1)
            ExifSnapshot(ExifSummary.build(raw, lat, lon), lat, lon, raw["DateTimeOriginal"])
        }
    }
}
