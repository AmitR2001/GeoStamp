// geostamp-iteration: 6
package com.example.geostamp.data.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import java.io.IOException

class SavedImage(val uri: Uri, val displayName: String, val relativePath: String)

class StoredInfo(val sizeBytes: Long, val width: Int, val height: Int, val relativePath: String, val displayName: String)

/**
 * Publishes and manages JPEGs in shared storage (Pictures/GeoStamp/<folder>) with MediaStore, so
 * they show up in the phone's gallery and file manager. No storage permission is needed on
 * Android 10+ for files this app created. Call from a background thread.
 */
class MediaStoreSaver(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    private fun relative(folder: String) = "${Environment.DIRECTORY_PICTURES}/GeoStamp/$folder"

    /** [afterWrite] runs while the file is still hidden (pending), e.g. to copy EXIF metadata. */
    fun saveJpeg(
        bitmap: Bitmap,
        displayName: String,
        folder: String,
        quality: Int = 95,
        afterWrite: ((Uri) -> Unit)? = null
    ): SavedImage {
        val relativePath = relative(folder)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, values)
            ?: throw IOException("Android would not create the file. Storage may be full.")

        try {
            val stream = resolver.openOutputStream(uri, "w")
                ?: throw IOException("Could not open the new file for writing.")
            stream.use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)) {
                    throw IOException("Could not write the JPEG. Storage may be full.")
                }
            }
            afterWrite?.invoke(uri)
            val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)
        } catch (t: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw t
        }
        return describe(uri, displayName, relativePath)
    }

    /**
     * Replaces the picture inside an existing saved file. The JPEG is built in memory first so a
     * failure to encode never touches the old file.
     */
    fun overwriteJpeg(uri: Uri, bitmap: Bitmap, quality: Int = 95, afterWrite: ((Uri) -> Unit)? = null) {
        val buffer = ByteArrayOutputStream()
        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, buffer)) {
            throw IOException("Could not encode the JPEG. Nothing was changed.")
        }
        val bytes = buffer.toByteArray()
        val stream = resolver.openOutputStream(uri, "wt")
            ?: throw IOException("Could not open the saved photo for writing.")
        stream.use { it.write(bytes) }
        afterWrite?.invoke(uri)
    }

    /** Copies an existing saved photo byte for byte into a NEW file; [afterWrite] may then adjust the new copy. */
    fun copyJpeg(source: Uri, displayName: String, folder: String, afterWrite: ((Uri) -> Unit)? = null): SavedImage {
        val relativePath = relative(folder)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, values)
            ?: throw IOException("Android would not create the file. Storage may be full.")
        try {
            val input = resolver.openInputStream(source) ?: throw IOException("Could not read the saved photo.")
            input.use { from ->
                val output = resolver.openOutputStream(uri, "w") ?: throw IOException("Could not open the new file for writing.")
                output.use { to -> from.copyTo(to) }
            }
            afterWrite?.invoke(uri)
            val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)
        } catch (t: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw t
        }
        return describe(uri, displayName, relativePath)
    }

    /** Returns true when a row was removed. Throws SecurityException for files this install did not create. */
    fun delete(uri: Uri): Boolean = resolver.delete(uri, null, null) > 0

    fun moveTo(uri: Uri, folder: String): SavedImage {
        val values = ContentValues().apply { put(MediaStore.MediaColumns.RELATIVE_PATH, relative(folder)) }
        if (resolver.update(uri, values, null, null) <= 0) throw IOException("Android did not move the file.")
        return describe(uri, "", relative(folder))
    }

    fun rename(uri: Uri, displayName: String): SavedImage {
        val values = ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, displayName) }
        if (resolver.update(uri, values, null, null) <= 0) throw IOException("Android did not rename the file.")
        return describe(uri, displayName, "")
    }

    fun info(uri: Uri): StoredInfo? {
        val projection = arrayOf(
            MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.WIDTH, MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.RELATIVE_PATH, MediaStore.MediaColumns.DISPLAY_NAME
        )
        return try {
            resolver.query(uri, projection, null, null, null)?.use { c ->
                if (c.moveToFirst()) StoredInfo(c.getLong(0), c.getInt(1), c.getInt(2), c.getString(3) ?: "", c.getString(4) ?: "")
                else null
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Reads back what Android actually stored, as a check that the file is in the gallery index. */
    private fun describe(uri: Uri, fallbackName: String, fallbackPath: String): SavedImage {
        val info = info(uri)
        return SavedImage(
            uri,
            info?.displayName?.ifEmpty { null } ?: fallbackName,
            info?.relativePath?.ifEmpty { null } ?: fallbackPath
        )
    }
}
