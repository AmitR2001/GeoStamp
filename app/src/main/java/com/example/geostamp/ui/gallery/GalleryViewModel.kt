// geostamp-iteration: 8
package com.example.geostamp.ui.gallery

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.geostamp.app.AppServices
import com.example.geostamp.data.media.ImportedPhoto
import com.example.geostamp.domain.album.AlbumNames
import com.example.geostamp.domain.metadata.FileSizes
import com.example.geostamp.domain.model.EditorSession
import com.example.geostamp.domain.model.PhotoRecord
import com.example.geostamp.domain.model.PhotoSource
import com.example.geostamp.domain.selection.Selection
import com.example.geostamp.domain.stamp.LocationSource
import com.example.geostamp.domain.stamp.StampContent
import com.example.geostamp.domain.stamp.StampFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

data class GalleryState(
    val loading: Boolean = true,
    val photos: List<PhotoRecord> = emptyList(),
    val selectedAlbum: String? = null,
    val message: String? = null,
    val busy: Boolean = false,
    val revision: Int = 0,
    val selection: Selection = Selection(),
    val progressText: String? = null
) {
    val albums: List<String> get() = AlbumNames.distinctSorted(photos.map { it.album })
    val visible: List<PhotoRecord>
        get() = if (selectedAlbum == null) photos else photos.filter { AlbumNames.same(it.album, selectedAlbum) }
}

class PhotoDetails(
    val name: String,
    val location: String,
    val size: String,
    val dimensions: String,
    val source: String,
    val album: String,
    val stampSource: String,
    val stampLines: List<String>
)

class GalleryViewModel(app: Application) : AndroidViewModel(app) {
    private val services = AppServices.get(app)
    private val _state = MutableStateFlow(GalleryState())
    val state: StateFlow<GalleryState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val photos = services.library.list()
            _state.update {
                it.copy(
                    loading = false,
                    photos = photos,
                    revision = it.revision + 1,
                    selection = it.selection.retainOnly(photos.map { p -> p.id })
                )
            }
        }
    }

    fun selectAlbum(album: String?) = _state.update { it.copy(selectedAlbum = album) }

    fun clearMessage() = _state.update { it.copy(message = null) }

    suspend fun thumbnail(record: PhotoRecord): Bitmap? = services.thumbnails.get(record)

    suspend fun loadFull(record: PhotoRecord): Result<Bitmap> = withContext(Dispatchers.IO) {
        runCatching {
            val uri = record.savedUri
            val original = record.originalFileName
            when {
                uri != null -> services.bitmapLoader.loadUri(Uri.parse(uri), FULL_EDGE).bitmap
                original != null ->
                    services.bitmapLoader.loadFile(File(services.originalsDir, original), FULL_EDGE, record.rotationDegrees).bitmap
                else -> throw java.io.IOException("This photo has no picture to show.")
            }
        }
    }

    suspend fun details(record: PhotoRecord): PhotoDetails = withContext(Dispatchers.IO) {
        val info = record.savedUri?.let { services.mediaSaver.info(Uri.parse(it)) }
        val stampLines = record.content?.let { content ->
            record.config?.let { config -> StampFormatter.format(content, config, Locale.getDefault()) }
        } ?: emptyList()
        PhotoDetails(
            name = record.displayName,
            location = record.savedPath ?: "Not saved to the gallery",
            size = info?.let { FileSizes.human(it.sizeBytes) } ?: "Unknown",
            dimensions = if (info != null && info.width > 0) "${info.width} x ${info.height} px" else "Unknown",
            source = when (record.source) {
                PhotoSource.CAMERA -> "Taken with GeoStamp"
                PhotoSource.IMPORTED -> "Imported photo"
                PhotoSource.EDITED -> "Edited copy"
            },
            album = record.album ?: "None",
            stampSource = when (record.content?.locationSource) {
                LocationSource.GPS -> "Location came from the phone (GPS)"
                LocationSource.MANUAL -> "Location was typed by you"
                LocationSource.UNAVAILABLE, null -> "No location on the stamp"
            },
            stampLines = stampLines
        )
    }

    // ---- editing sessions ----------------------------------------------------------------

    /** Builds an editor session for a saved photo, or null when its private original is gone. */
    fun sessionFor(record: PhotoRecord): EditorSession? {
        val name = record.originalFileName ?: return null
        val file = File(services.originalsDir, name)
        if (!file.exists()) return null
        val content = record.content ?: StampContent(
            capturedAt = Instant.ofEpochMilli(record.createdAtMillis),
            zoneId = ZoneId.systemDefault()
        )
        return EditorSession(
            id = record.id,
            originalFile = file,
            capturedAt = content.capturedAt,
            zoneId = content.zoneId,
            location = null,
            source = record.source,
            initialContent = content,
            initialConfig = record.config,
            existing = record,
            rotationDegrees = record.rotationDegrees,
            infoNote = "This photo already has a saved stamp. Change anything below, then save a copy."
        )
    }

    // ---- selecting several photos -------------------------------------------------------

    fun toggleSelected(id: String) = _state.update { it.copy(selection = it.selection.toggle(id)) }
    fun selectAllVisible() = _state.update { it.copy(selection = it.selection.selectAll(it.visible.map { p -> p.id })) }
    fun clearSelection() = _state.update { it.copy(selection = it.selection.clear()) }

    private fun selectedRecords(): List<PhotoRecord> {
        val s = _state.value
        return s.photos.filter { s.selection.contains(it.id) }
    }

    fun selectedUris(): List<Uri> = selectedRecords().mapNotNull { r -> r.savedUri?.let { Uri.parse(it) } }

    /** Editor sessions for the ticked photos; photos whose private original is missing are skipped and counted. */
    fun sessionsForSelected(): Pair<List<EditorSession>, Int> {
        val records = selectedRecords()
        val sessions = records.mapNotNull { sessionFor(it) }
        return sessions to (records.size - sessions.size)
    }

    fun deleteSelected() = runAction("The photos could not be deleted.") {
        val records = selectedRecords()
        var deleted = 0
        var failed = 0
        records.forEachIndexed { index, record ->
            _state.update { it.copy(progressText = "Deleting ${index + 1} of ${records.size}...") }
            try {
                services.library.delete(record)
                deleted++
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                failed++
            }
        }
        _state.update { it.copy(progressText = null, selection = Selection()) }
        refresh()
        say(if (failed == 0) "Deleted $deleted photo(s)." else "Deleted $deleted. $failed could not be deleted (Android did not allow it).")
    }

    fun moveSelectedToAlbum(typedName: String?) = runAction("The album could not be changed.") {
        val album = typedName?.let { AlbumNames.sanitizeAlbum(it) }
        if (typedName != null && album == null) {
            say("Album names can use letters, numbers, spaces, - and _.")
            return@runAction
        }
        val records = selectedRecords()
        var moved = 0
        records.forEachIndexed { index, record ->
            _state.update { it.copy(progressText = "Moving ${index + 1} of ${records.size}...") }
            val result = services.library.setAlbum(record, album)
            if (result.movedInStorage || album == null) moved++
        }
        _state.update { it.copy(progressText = null, selection = Selection()) }
        refresh()
        say(
            when {
                album == null -> "Took ${records.size} photo(s) out of their album."
                moved == records.size -> "Moved ${records.size} photo(s) to $album."
                else -> "Added ${records.size} photo(s) to the album \"$album\". Android did not allow moving some of the files."
            }
        )
    }

    fun exportSelected(folder: Uri) = runAction("The photos could not be exported.") {
        val records = selectedRecords().filter { it.savedUri != null }
        if (records.isEmpty()) {
            say("None of the ticked photos has a saved file to export.")
            return@runAction
        }
        var exported = 0
        var failed = 0
        records.forEachIndexed { index, record ->
            _state.update { it.copy(progressText = "Exporting ${index + 1} of ${records.size}...") }
            try {
                withContext(Dispatchers.IO) {
                    services.folderExporter.export(Uri.parse(record.savedUri!!), folder, record.displayName)
                }
                exported++
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                failed++
            }
        }
        _state.update { it.copy(progressText = null) }
        say(if (failed == 0) "Exported $exported photo(s) to the folder you picked." else "Exported $exported. $failed could not be exported.")
    }

    /** Copies several picked photos into the app; the editor pages open once they are all in. */
    fun importPhotos(uris: List<Uri>, onSessions: (List<EditorSession>) -> Unit) {
        if (uris.isEmpty() || _state.value.busy) return
        _state.update { it.copy(busy = true, progressText = "Importing 1 of ${uris.size}...") }
        viewModelScope.launch {
            val sessions = mutableListOf<EditorSession>()
            var failed = 0
            var lastError: String? = null
            uris.forEachIndexed { index, uri ->
                _state.update { it.copy(progressText = "Importing ${index + 1} of ${uris.size}...") }
                try {
                    val imported = withContext(Dispatchers.IO) { services.importCopier.copy(uri) }
                    sessions += sessionFor(imported)
                } catch (t: Throwable) {
                    if (t is CancellationException) throw t
                    failed++
                    lastError = t.message
                }
            }
            _state.update {
                it.copy(
                    busy = false,
                    progressText = null,
                    message = if (failed > 0) "$failed photo(s) could not be imported" + (lastError?.let { e -> ": $e" } ?: ".") else it.message
                )
            }
            if (sessions.isNotEmpty()) onSessions(sessions)
        }
    }

    fun importPhoto(uri: Uri, onSession: (EditorSession) -> Unit) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                val imported = withContext(Dispatchers.IO) { services.importCopier.copy(uri) }
                _state.update { it.copy(busy = false) }
                onSession(sessionFor(imported))
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _state.update { it.copy(busy = false, message = t.message ?: "The photo could not be imported.") }
            }
        }
    }

    private fun sessionFor(imported: ImportedPhoto): EditorSession {
        val hasGps = imported.latitude != null && imported.longitude != null
        val content = StampContent(
            latitude = imported.latitude,
            longitude = imported.longitude,
            locationSource = if (hasGps) LocationSource.GPS else LocationSource.UNAVAILABLE,
            capturedAt = imported.capturedAt,
            zoneId = imported.zone
        )
        val note = (if (imported.dateFromMetadata) "The date and time were read from the photo's own metadata."
        else "This photo has no capture date, so the current time was filled in. Change it below if you know the real one.") +
            (if (hasGps) " The coordinates were read from the photo's own metadata."
            else " The photo has no location. Type one below if you want.")
        return EditorSession(
            id = imported.id,
            originalFile = imported.file,
            capturedAt = imported.capturedAt,
            zoneId = imported.zone,
            location = null,
            source = PhotoSource.IMPORTED,
            initialContent = content,
            infoNote = note
        )
    }

    // ---- actions on one photo ------------------------------------------------------------

    fun delete(record: PhotoRecord, onDone: () -> Unit) = runAction("The photo could not be deleted.") {
        services.library.delete(record)
        refresh()
        say("Photo deleted.")
        onDone()
    }

    fun removeFromList(record: PhotoRecord, onDone: () -> Unit) = runAction("The photo could not be removed.") {
        services.library.removeFromList(record)
        refresh()
        say("Removed from GeoStamp. Nothing in your gallery was touched.")
        onDone()
    }

    fun setAlbum(record: PhotoRecord, typedName: String?) = runAction("The album could not be changed.") {
        val album = typedName?.let { AlbumNames.sanitizeAlbum(it) }
        if (typedName != null && album == null) {
            say("Album names can use letters, numbers, spaces, - and _.")
            return@runAction
        }
        val result = services.library.setAlbum(record, album)
        refresh()
        say(
            when {
                album == null -> "Removed from the album."
                result.movedInStorage -> "Moved to Pictures/GeoStamp/$album."
                else -> "Added to the album \"$album\" inside GeoStamp. Android did not allow moving the file itself."
            }
        )
    }

    fun rename(record: PhotoRecord, typedName: String) = runAction("The photo could not be renamed.") {
        val name = AlbumNames.sanitizeFileName(typedName)
        if (name == null) {
            say("Please type a file name.")
            return@runAction
        }
        services.library.rename(record, name)
        refresh()
        say("Renamed to $name.")
    }

    fun export(record: PhotoRecord, folder: Uri) = runAction("The photo could not be exported.") {
        val uriText = record.savedUri
        if (uriText == null) {
            say("This photo has no saved file to export.")
            return@runAction
        }
        withContext(Dispatchers.IO) { services.folderExporter.export(Uri.parse(uriText), folder, record.displayName) }
        say("Exported a copy to the folder you picked.")
    }

    fun say(text: String) = _state.update { it.copy(message = text) }

    private fun runAction(failure: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                say(t.message ?: failure)
            }
        }
    }

    private companion object {
        const val FULL_EDGE = 2048
    }
}
