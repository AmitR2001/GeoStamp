package com.example.geostamp.data.media

import android.content.Context
import android.net.Uri
import com.example.geostamp.data.store.PhotoStore
import com.example.geostamp.domain.metadata.ExifChanges
import com.example.geostamp.domain.model.PhotoRecord
import com.example.geostamp.domain.stamp.StampFileNames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * The only place that edits a photo's hidden metadata. It always works on a NEW COPY of the saved
 * photo, so the existing file is never changed by a metadata edit.
 */
class MetadataService(
    private val context: Context,
    private val reader: ExifReader,
    private val media: MediaStoreSaver,
    private val store: PhotoStore
) {
    suspend fun read(record: PhotoRecord): ExifSnapshot = withContext(Dispatchers.IO) {
        val uri = record.savedUri ?: throw IOException("This photo has no saved file to inspect.")
        reader.read(Uri.parse(uri))
    }

    suspend fun saveCopy(record: PhotoRecord, changes: ExifChanges): PhotoRecord = withContext(Dispatchers.IO) {
        val source = record.savedUri?.let { Uri.parse(it) } ?: throw IOException("This photo has no saved file to copy.")
        if (changes.isEmpty) throw IOException("There is nothing to change.")
        val baseName = record.displayName.removeSuffix(".jpg").take(60)
        val name = "${baseName}_metadata_${StampFileNames.newId().take(4)}.jpg"
        var written = false
        val saved = media.copyJpeg(source, name, PhotoSaver.FOLDER_EDITED) { uri ->
            val pfd = context.contentResolver.openFileDescriptor(uri, "rw")
                ?: throw IOException("Could not open the new copy for writing metadata.")
            pfd.use { ExifWriter.apply(it, changes) }
            written = true
        }
        if (!written) throw IOException("The metadata could not be written.")
        val updated = record.copy(
            id = StampFileNames.newId(),
            createdAtMillis = System.currentTimeMillis(),
            savedUri = saved.uri.toString(),
            savedPath = saved.relativePath + saved.displayName,
            displayName = saved.displayName
        )
        store.upsert(updated)
        updated
    }
}
