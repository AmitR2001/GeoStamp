// geostamp-iteration: 7
package com.example.geostamp.data.media

import android.content.Context
import android.net.Uri
import com.example.geostamp.data.maps.MapTileProvider
import com.example.geostamp.data.render.StampRenderer
import com.example.geostamp.data.store.PhotoStore
import com.example.geostamp.domain.model.EditorSession
import com.example.geostamp.domain.model.PhotoRecord
import com.example.geostamp.domain.model.PhotoSource
import com.example.geostamp.domain.stamp.StampConfig
import com.example.geostamp.domain.stamp.StampContent
import com.example.geostamp.domain.stamp.StampFileNames
import com.example.geostamp.domain.stamp.StampFormatter
import com.example.geostamp.domain.stamp.StampLayoutStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

enum class SaveMode { NEW_COPY, REPLACE }

class SaveOutcome(
    val saved: SavedImage,
    val record: PhotoRecord,
    val exifCopied: Boolean,
    val replaced: Boolean,
    val mapMissing: Boolean = false
)

/**
 * Renders the stamp onto the full-size ORIGINAL (kept untouched in private storage), publishes
 * the result to Pictures/GeoStamp, and remembers the stamp settings for later editing.
 * Default is a new copy; replacing an existing saved photo is a separate, explicit mode.
 */
class PhotoSaver(
    private val context: Context,
    private val loader: BitmapLoader,
    private val mediaSaver: MediaStoreSaver,
    private val store: PhotoStore,
    private val thumbnails: ThumbnailCache,
    private val mapTiles: MapTileProvider
) {
    fun folderFor(session: EditorSession): String =
        if (session.source == PhotoSource.CAMERA && session.existing == null) FOLDER_CAMERA else FOLDER_EDITED

    suspend fun save(
        session: EditorSession,
        content: StampContent,
        config: StampConfig,
        rotationDegrees: Int,
        mode: SaveMode
    ): SaveOutcome = withContext(Dispatchers.IO) {
        val existing = session.existing
        val replaceTarget = existing?.savedUri?.let { Uri.parse(it) }
        if (mode == SaveMode.REPLACE && (existing == null || replaceTarget == null)) {
            throw IOException("There is no saved photo to replace.")
        }

        val wantsMap = config.layoutStyle == StampLayoutStyle.CARD && config.showMap &&
            StampFormatter.hasValidCoordinates(content)
        val map = if (wantsMap) mapTiles.squareMap(content.latitude!!, content.longitude!!) else null
        val mapMissing = wantsMap && map == null

        val loaded = loader.loadFile(session.originalFile, MAX_SAVE_LONG_EDGE, rotationDegrees)
        val stamped = try {
            StampRenderer.renderStamp(loaded.bitmap, content, config, map)
        } finally {
            loaded.bitmap.recycle()
            map?.recycle()
        }
        try {
            var exifCopied = false
            if (mode == SaveMode.REPLACE && existing != null && replaceTarget != null) {
                mediaSaver.overwriteJpeg(replaceTarget, stamped) { uri -> exifCopied = copyExif(session, uri) }
                val info = mediaSaver.info(replaceTarget)
                val record = existing.copy(
                    content = content,
                    config = config,
                    rotationDegrees = rotationDegrees,
                    savedPath = if (info != null) info.relativePath + info.displayName else existing.savedPath
                )
                store.upsert(record)
                thumbnails.invalidate(existing.id)
                SaveOutcome(
                    SavedImage(replaceTarget, record.displayName, record.savedPath.orEmpty()),
                    record, exifCopied, replaced = true, mapMissing = mapMissing
                )
            } else {
                val id = if (existing == null) session.id else StampFileNames.newId()
                val name = StampFileNames.forPhoto(session.capturedAt, session.zoneId, id)
                val saved = mediaSaver.saveJpeg(stamped, name, folderFor(session)) { uri ->
                    exifCopied = copyExif(session, uri)
                }
                val record = PhotoRecord(
                    id = id,
                    createdAtMillis = System.currentTimeMillis(),
                    source = existing?.source ?: session.source,
                    originalFileName = session.originalFile.name,
                    savedUri = saved.uri.toString(),
                    savedPath = saved.relativePath + saved.displayName,
                    displayName = saved.displayName,
                    album = existing?.album,
                    content = content,
                    config = config,
                    rotationDegrees = rotationDegrees
                )
                store.upsert(record)
                SaveOutcome(saved, record, exifCopied, replaced = false, mapMissing = mapMissing)
            }
        } finally {
            stamped.recycle()
        }
    }

    private fun copyExif(session: EditorSession, uri: Uri): Boolean {
        val pfd = context.contentResolver.openFileDescriptor(uri, "rw") ?: return false
        return pfd.use { descriptor ->
            session.originalFile.inputStream().use { input -> ExifCopier.copy(input, descriptor) }
        }
    }

    companion object {
        const val MAX_SAVE_LONG_EDGE = 4096
        const val FOLDER_CAMERA = "Camera"
        const val FOLDER_EDITED = "Edited"
    }
}
