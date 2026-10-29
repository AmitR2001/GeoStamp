package com.example.geostamp.data.media

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.example.geostamp.domain.metadata.ExifDates
import com.example.geostamp.domain.stamp.StampFileNames
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.ZoneId

class ImportedPhoto(
    val id: String,
    val file: File,
    val capturedAt: Instant,
    val zone: ZoneId,
    val dateFromMetadata: Boolean,
    val latitude: Double?,
    val longitude: Double?
)

/**
 * Copies a picked photo into the app's private storage so the original stays available for
 * editing, and reads the date and GPS the photo already carries. Call from a background thread.
 */
class ImportCopier(context: Context, private val originalsDir: File) {
    private val resolver = context.applicationContext.contentResolver

    fun copy(uri: Uri): ImportedPhoto {
        originalsDir.mkdirs()
        val id = StampFileNames.newId()
        val extension = when (resolver.getType(uri)) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/heic", "image/heif" -> "heic"
            else -> "img"
        }
        val file = File(originalsDir, "$id.$extension")
        try {
            val input = resolver.openInputStream(uri) ?: throw IOException("Could not open the selected photo.")
            input.use { source -> file.outputStream().use { target -> source.copyTo(target) } }

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                throw IOException("That file is not a photo GeoStamp can read.")
            }
            return readMetadata(id, file)
        } catch (t: Throwable) {
            runCatching { file.delete() }
            if (t is IOException) throw t
            throw IOException("The photo could not be imported: ${t.message}")
        }
    }

    private fun readMetadata(id: String, file: File): ImportedPhoto {
        val fallback = ZoneId.systemDefault()
        return try {
            val exif = ExifInterface(file.absolutePath)
            val time = ExifDates.parse(
                exif.getAttribute("DateTimeOriginal") ?: exif.getAttribute("DateTime"),
                exif.getAttribute("OffsetTimeOriginal") ?: exif.getAttribute("OffsetTime"),
                fallback
            )
            val latLong = exif.latLong
            ImportedPhoto(
                id = id,
                file = file,
                capturedAt = time?.instant ?: Instant.now(),
                zone = time?.zone ?: fallback,
                dateFromMetadata = time != null,
                latitude = latLong?.get(0),
                longitude = latLong?.get(1)
            )
        } catch (e: Exception) {
            ImportedPhoto(id, file, Instant.now(), fallback, false, null, null)
        }
    }
}
