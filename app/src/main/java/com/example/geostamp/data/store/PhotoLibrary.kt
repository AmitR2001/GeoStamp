package com.example.geostamp.data.store

import android.net.Uri
import com.example.geostamp.data.media.MediaStoreSaver
import com.example.geostamp.domain.model.PhotoRecord
import com.example.geostamp.domain.model.PhotoSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class AlbumResult(val record: PhotoRecord, val movedInStorage: Boolean)

/**
 * Everything the gallery can do to a photo: list, delete, move to an album, rename.
 * Public files are only touched through MediaStore, and failures are reported, never hidden.
 */
class PhotoLibrary(
    private val store: PhotoStore,
    private val media: MediaStoreSaver,
    private val originalsDir: File
) {
    suspend fun list(): List<PhotoRecord> = store.all().sortedByDescending { it.createdAtMillis }

    /** Removes the saved copy from the phone's gallery, then the record, then the private original. */
    suspend fun delete(record: PhotoRecord) = withContext(Dispatchers.IO) {
        val uriText = record.savedUri
        if (uriText != null) {
            try {
                media.delete(Uri.parse(uriText))
            } catch (e: SecurityException) {
                throw IOException(
                    "Android did not allow GeoStamp to delete this file. Delete it in your gallery app, " +
                        "then use Remove from GeoStamp."
                )
            }
        }
        removeRecord(record)
    }

    /** Forgets the photo in GeoStamp only (used when the saved file is already gone). */
    suspend fun removeFromList(record: PhotoRecord) = withContext(Dispatchers.IO) { removeRecord(record) }

    private suspend fun removeRecord(record: PhotoRecord) {
        store.delete(record.id)
        val original = record.originalFileName ?: return
        val stillUsed = store.all().any { it.originalFileName == original }
        if (!stillUsed) runCatching { File(originalsDir, original).delete() }
    }

    suspend fun setAlbum(record: PhotoRecord, album: String?): AlbumResult = withContext(Dispatchers.IO) {
        val folder = album ?: if (record.source == PhotoSource.CAMERA) "Camera" else "Edited"
        var path = record.savedPath
        var moved = false
        val uriText = record.savedUri
        if (uriText != null) {
            try {
                val saved = media.moveTo(Uri.parse(uriText), folder)
                path = saved.relativePath + record.displayName
                moved = true
            } catch (e: Exception) {
                moved = false
            }
        }
        val updated = record.copy(album = album, savedPath = path)
        store.upsert(updated)
        AlbumResult(updated, moved)
    }

    suspend fun rename(record: PhotoRecord, newName: String): PhotoRecord = withContext(Dispatchers.IO) {
        val uriText = record.savedUri ?: throw IOException("This photo has no saved file to rename.")
        val saved = try {
            media.rename(Uri.parse(uriText), newName)
        } catch (e: SecurityException) {
            throw IOException("Android did not allow GeoStamp to rename this file.")
        } catch (e: Exception) {
            throw IOException("The file could not be renamed: ${e.message ?: "unknown error"}")
        }
        val name = saved.displayName.ifEmpty { newName }
        val updated = record.copy(
            displayName = name,
            savedPath = if (saved.relativePath.isNotEmpty()) saved.relativePath + name else record.savedPath
        )
        store.upsert(updated)
        updated
    }
}
