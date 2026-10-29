package com.example.geostamp.ui.batch

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.geostamp.app.AppServices
import com.example.geostamp.data.media.SaveMode
import com.example.geostamp.data.render.StampRenderer
import com.example.geostamp.domain.batch.BatchEditing
import com.example.geostamp.domain.batch.BatchErrors
import com.example.geostamp.domain.batch.BatchFields
import com.example.geostamp.domain.model.EditorSession
import com.example.geostamp.domain.places.PlaceSearch
import com.example.geostamp.domain.places.SavedPlace
import com.example.geostamp.domain.stamp.StampConfig
import com.example.geostamp.domain.stamp.StampContent
import com.example.geostamp.domain.stamp.StampEditing
import com.example.geostamp.domain.stamp.StampFileNames
import com.example.geostamp.domain.stamp.StampFormatter
import com.example.geostamp.domain.stamp.StampLayoutStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class BatchResultInfo(val saved: Int, val failed: Int, val problems: List<String>, val withoutMap: Int)

data class BatchState(
    val count: Int = 0,
    val loading: Boolean = true,
    val preview: Bitmap? = null,
    val fields: BatchFields = BatchFields(),
    val errors: BatchErrors = BatchErrors(),
    val config: StampConfig = StampConfig(),
    val suggestions: List<SavedPlace> = emptyList(),
    val message: String? = null,
    val canSavePlace: Boolean = false,
    val running: Boolean = false,
    val progress: Int = 0,
    val result: BatchResultInfo? = null,
    val mapNote: String? = null,
    val mapFailed: Boolean = false
)

/**
 * "Stamp many photos": one place, note and style applied to every photo, each saved as its own new
 * copy. Every photo keeps its own date and time unless one date is set for all.
 */
class BatchViewModel(app: Application) : AndroidViewModel(app) {
    private val services = AppServices.get(app)
    private val _state = MutableStateFlow(BatchState())
    val state: StateFlow<BatchState> = _state.asStateFlow()

    private var startedKey: String? = null
    private var sessions: List<EditorSession> = emptyList()
    private var fields = BatchFields()
    private var baseBitmap: Bitmap? = null
    private var previewBase: StampContent? = null
    private var places: List<SavedPlace> = emptyList()
    private var renderJob: Job? = null
    private var mapJob: Job? = null
    private var mapBitmap: Bitmap? = null
    private var mapKey: String? = null

    fun start(list: List<EditorSession>) {
        val key = list.joinToString(",") { it.id }
        if (startedKey == key) return
        startedKey = key
        sessions = list

        val first = list.first()
        val base = first.initialContent ?: StampContent(capturedAt = first.capturedAt, zoneId = first.zoneId)
        previewBase = base
        val defaults = StampEditing.fieldsFrom(base)
        fields = BatchFields(date = defaults.date, time = defaults.time, zone = defaults.zone)
        _state.value = BatchState(
            count = list.size,
            loading = true,
            fields = fields,
            config = first.initialConfig ?: services.stampPrefs.load()
        )
        viewModelScope.launch { places = services.placeStore.all() }
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                runCatching { services.bitmapLoader.loadFile(first.originalFile, PREVIEW_LONG_EDGE, first.rotationDegrees) }
            }
            loaded.onSuccess {
                baseBitmap = it.bitmap
                _state.update { s -> s.copy(loading = false) }
                refreshPreview()
            }.onFailure { e ->
                if (e !is CancellationException) {
                    _state.update { s -> s.copy(loading = false, message = e.message ?: "The first photo could not be opened.") }
                }
            }
        }
    }

    // ---- editing ----------------------------------------------------------------------

    fun setPlaceName(value: String) {
        change { it.copy(placeName = value) }
        val matches = PlaceSearch.search(value, places).filterNot { it.name.equals(value.trim(), ignoreCase = true) }
        _state.update { it.copy(suggestions = matches, message = null) }
    }

    fun dismissSuggestions() = _state.update { it.copy(suggestions = emptyList()) }
    fun setLatitude(v: String) = change { it.copy(latitude = v) }
    fun setLongitude(v: String) = change { it.copy(longitude = v) }
    fun setAddress(v: String) = change { it.copy(address = v) }
    fun setLabel(v: String) = change { it.copy(label = v) }
    fun setSetDateTime(on: Boolean) = change { it.copy(setDateTime = on) }
    fun setDate(v: String) = change { it.copy(date = v) }
    fun setTime(v: String) = change { it.copy(time = v) }
    fun setZone(v: String) = change { it.copy(zone = v) }

    fun applyPlace(place: SavedPlace) {
        change {
            it.copy(
                placeName = place.name,
                latitude = StampEditing.formatCoordinate(place.latitude),
                longitude = StampEditing.formatCoordinate(place.longitude),
                address = place.address.orEmpty()
            )
        }
        _state.update { it.copy(suggestions = emptyList(), message = null) }
        if (place.address == null) {
            viewModelScope.launch {
                val found = services.addressResolver.resolve(place.latitude, place.longitude)
                val stillThisPlace = fields.placeName == place.name && fields.address.isBlank()
                if (found != null && stillThisPlace) {
                    change { it.copy(address = found) }
                    services.placeStore.upsert(place.copy(address = found))
                    places = services.placeStore.all()
                }
            }
        }
    }

    fun savePlaceFromCurrent() {
        val name = fields.placeName.trim()
        val lat = StampEditing.parseLatitude(fields.latitude)
        val lon = StampEditing.parseLongitude(fields.longitude)
        if (name.isEmpty() || lat == null || lon == null) {
            _state.update { it.copy(message = "Type a place name and valid coordinates first.") }
            return
        }
        viewModelScope.launch {
            val existing = places.firstOrNull { it.name.equals(name, ignoreCase = true) }
            val place = (existing ?: SavedPlace(StampFileNames.newId(), name, lat, lon)).copy(
                name = name, latitude = lat, longitude = lon,
                address = fields.address.trim().ifEmpty { null } ?: existing?.address
            )
            services.placeStore.upsert(place)
            places = services.placeStore.all()
            _state.update { it.copy(message = "Saved \"$name\" in your places.") }
        }
    }

    fun updateConfig(transform: (StampConfig) -> StampConfig) {
        _state.update { it.copy(config = transform(it.config)) }
        refreshPreview()
    }

    fun retryMap() {
        mapKey = null
        refreshPreview()
    }

    private fun change(block: (BatchFields) -> BatchFields) {
        fields = block(fields)
        val validation = BatchEditing.validate(fields)
        _state.update {
            it.copy(
                fields = fields,
                errors = validation.errors,
                canSavePlace = fields.placeName.isNotBlank() &&
                    StampEditing.parseLatitude(fields.latitude) != null &&
                    StampEditing.parseLongitude(fields.longitude) != null
            )
        }
        refreshPreview()
    }

    // ---- preview of the first photo -----------------------------------------------------

    private fun previewContent(): StampContent? {
        val base = previewBase ?: return null
        val options = BatchEditing.validate(fields).options ?: return base
        return BatchEditing.apply(base, options)
    }

    private fun wantsMap(config: StampConfig, content: StampContent) =
        config.layoutStyle == StampLayoutStyle.CARD && config.showMap && StampFormatter.hasValidCoordinates(content)

    private fun refreshPreview() {
        val content = previewContent() ?: return
        val config = _state.value.config
        if (wantsMap(config, content)) {
            val lat = content.latitude
            val lon = content.longitude
            if (lat != null && lon != null) {
                val key = String.format(Locale.US, "%.5f,%.5f", lat, lon)
                if (key != mapKey) {
                    mapKey = key
                    mapBitmap = null
                    mapJob?.cancel()
                    _state.update { it.copy(mapNote = "Downloading the map picture...", mapFailed = false) }
                    mapJob = viewModelScope.launch {
                        val bitmap = services.mapTiles.squareMap(lat, lon)
                        if (mapKey == key) {
                            mapBitmap = bitmap
                            _state.update {
                                it.copy(
                                    mapNote = if (bitmap == null) "The map picture could not be downloaded, so the cards have no map." else null,
                                    mapFailed = bitmap == null
                                )
                            }
                            render()
                        }
                    }
                }
            }
        } else {
            _state.update { it.copy(mapNote = null, mapFailed = false) }
        }
        render()
    }

    private fun render() {
        val base = baseBitmap ?: return
        val content = previewContent() ?: return
        val config = _state.value.config
        val map = if (wantsMap(config, content)) mapBitmap else null
        renderJob?.cancel()
        renderJob = viewModelScope.launch(Dispatchers.Default) {
            delay(30)
            val rendered = StampRenderer.renderStamp(base, content, config, map)
            _state.update { it.copy(preview = rendered) }
        }
    }

    // ---- stamping every photo -----------------------------------------------------------

    fun run() {
        if (_state.value.running) return
        val validation = BatchEditing.validate(fields)
        val options = validation.options
        if (options == null) {
            _state.update { it.copy(errors = validation.errors, message = "Fix the fields marked in red first.") }
            return
        }
        val config = _state.value.config
        _state.update { it.copy(running = true, progress = 0, result = null, message = null) }
        viewModelScope.launch {
            val lat = options.latitude
            val lon = options.longitude
            // One address lookup for the whole batch.
            val finalOptions = if (lat != null && lon != null && options.address == null) {
                val found = services.addressResolver.resolve(lat, lon)
                if (found != null) options.copy(address = found) else options
            } else {
                options
            }

            var saved = 0
            var failed = 0
            var withoutMap = 0
            val problems = mutableListOf<String>()
            sessions.forEachIndexed { index, session ->
                try {
                    val base = session.initialContent ?: StampContent(capturedAt = session.capturedAt, zoneId = session.zoneId)
                    val content = BatchEditing.apply(base, finalOptions)
                    val outcome = services.photoSaver.save(session, content, config, session.rotationDegrees, SaveMode.NEW_COPY)
                    saved++
                    if (outcome.mapMissing) withoutMap++
                } catch (t: Throwable) {
                    if (t is CancellationException) throw t
                    failed++
                    if (problems.size < 5) problems += "Photo ${index + 1}: ${t.message ?: "could not be saved"}"
                }
                _state.update { it.copy(progress = index + 1) }
            }
            services.stampPrefs.save(config)
            _state.update { it.copy(running = false, result = BatchResultInfo(saved, failed, problems, withoutMap)) }
        }
    }

    /** Leaving before stamping: remove the private working copies of photos that were just imported. */
    fun discardUnsaved() {
        if (_state.value.result != null || _state.value.running) return
        sessions.filter { it.existing == null }.forEach { runCatching { it.originalFile.delete() } }
    }

    private companion object {
        const val PREVIEW_LONG_EDGE = 1280
    }
}
