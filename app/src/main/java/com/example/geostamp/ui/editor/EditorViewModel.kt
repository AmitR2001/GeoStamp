// geostamp-iteration: 7
package com.example.geostamp.ui.editor

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.geostamp.app.AppServices
import com.example.geostamp.data.media.SaveMode
import com.example.geostamp.data.render.StampRenderer
import com.example.geostamp.domain.location.LocationResult
import com.example.geostamp.domain.maps.MapLocation
import com.example.geostamp.domain.maps.MapLookup
import com.example.geostamp.domain.places.PlaceSearch
import com.example.geostamp.domain.places.SavedPlace
import com.example.geostamp.domain.model.EditorSession
import com.example.geostamp.domain.stamp.FieldErrors
import com.example.geostamp.domain.stamp.LocationSource
import com.example.geostamp.domain.stamp.StampConfig
import com.example.geostamp.domain.stamp.StampContent
import com.example.geostamp.domain.stamp.StampEditing
import com.example.geostamp.domain.stamp.StampFields
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
import java.io.IOException
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class GpsLookup { NOT_STARTED, SEARCHING, FOUND, UNAVAILABLE }

class SaveResultInfo(
    val uri: Uri,
    val path: String,
    val displayName: String,
    val exifCopied: Boolean,
    val replaced: Boolean,
    val mapMissing: Boolean = false
)

data class EditorState(
    val loading: Boolean = true,
    val loadError: String? = null,
    val fields: StampFields = StampFields(),
    val errors: FieldErrors = FieldErrors(),
    val config: StampConfig = StampConfig(),
    val preview: Bitmap? = null,
    val source: LocationSource = LocationSource.UNAVAILABLE,
    val gpsLookup: GpsLookup = GpsLookup.NOT_STARTED,
    val gpsNote: String = "",
    val canUseGps: Boolean = false,
    val capturedText: String = "",
    val isExisting: Boolean = false,
    val canReplace: Boolean = false,
    val saveFolder: String = "Camera",
    val rotation: Int = 0,
    val saving: Boolean = false,
    val saveError: String? = null,
    val result: SaveResultInfo? = null,
    val mapBusy: Boolean = false,
    val mapMessage: String? = null,
    val placeSuggestions: List<SavedPlace> = emptyList(),
    val placeMessage: String? = null,
    val canSavePlace: Boolean = false,
    val mapNote: String? = null,
    val mapFailed: Boolean = false
)

class EditorViewModel(app: Application) : AndroidViewModel(app) {
    private val services = AppServices.get(app)
    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private var startedId: String? = null
    private lateinit var session: EditorSession
    private lateinit var capturedContent: StampContent
    private lateinit var originalContent: StampContent
    private lateinit var currentContent: StampContent
    private var fields = StampFields()
    private var rotation = 0
    private var baseBitmap: Bitmap? = null
    private var baseJob: Job? = null
    private var renderJob: Job? = null
    private var gpsFix: LocationResult.Fix? = null
    private var resolvedAddress: String? = null
    private var addressTouched = false
    private var locationTouched = false
    private var places: List<SavedPlace> = emptyList()
    private var mapBitmap: Bitmap? = null
    private var mapKey: String? = null
    private var mapJob: Job? = null

    fun start(session: EditorSession) {
        if (startedId == session.id) return
        startedId = session.id
        this.session = session

        val initial = session.initialContent ?: StampContent(capturedAt = session.capturedAt, zoneId = session.zoneId)
        capturedContent = initial
        originalContent = initial
        currentContent = initial
        fields = StampEditing.fieldsFrom(initial)
        rotation = session.rotationDegrees

        val looking = session.location != null
        _state.value = EditorState(
            loading = true,
            fields = fields,
            config = session.initialConfig ?: services.stampPrefs.load(),
            capturedText = describeCaptured(initial),
            gpsLookup = if (looking) GpsLookup.SEARCHING else GpsLookup.NOT_STARTED,
            gpsNote = when {
                looking -> "Looking for your location..."
                session.infoNote != null -> session.infoNote
                else -> "Location permission is off, so no location was looked up. You can type one below."
            },
            isExisting = session.existing != null,
            canReplace = session.existing?.savedUri != null,
            saveFolder = services.photoSaver.folderFor(session),
            rotation = rotation
        )

        loadBase()
        viewModelScope.launch { places = services.placeStore.all() }

        session.location?.let { deferred ->
            viewModelScope.launch {
                val result = try {
                    deferred.await()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    LocationResult.Unavailable(LocationResult.Reason.ERROR)
                }
                onLocationResult(result)
            }
        }

        // Imported photo that already carries coordinates: look up a readable address for them.
        val lat = initial.latitude
        val lon = initial.longitude
        if (lat != null && lon != null && initial.address == null && initial.locationSource == LocationSource.GPS) {
            viewModelScope.launch {
                val address = services.addressResolver.resolve(lat, lon)
                if (address != null && !addressTouched && !locationTouched) {
                    resolvedAddress = address
                    originalContent = originalContent.copy(address = address)
                    fields = fields.copy(address = address)
                    recompute()
                }
            }
        }
    }

    private fun loadBase() {
        baseJob?.cancel()
        _state.update { it.copy(loading = true) }
        baseJob = viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                runCatching { services.bitmapLoader.loadFile(session.originalFile, PREVIEW_LONG_EDGE, rotation) }
            }
            loaded.onSuccess {
                baseBitmap = it.bitmap
                _state.update { s -> s.copy(loading = false, loadError = null) }
                scheduleRender()
            }.onFailure { e ->
                if (e !is CancellationException) {
                    _state.update { s -> s.copy(loading = false, loadError = e.message ?: "The photo could not be opened.") }
                }
            }
        }
    }

    // ---- field edits -------------------------------------------------------------------

    fun setLabel(value: String) = mutateFields { it.copy(label = value) }
    fun setDate(value: String) = mutateFields { it.copy(date = value) }
    fun setTime(value: String) = mutateFields { it.copy(time = value) }
    fun setZone(value: String) = mutateFields { it.copy(zone = value) }

    fun setAddress(value: String) {
        addressTouched = true
        mutateFields { it.copy(address = value) }
    }

    fun setLatitude(value: String) {
        locationTouched = true
        mutateFields { it.copy(latitude = value) }
    }

    fun setLongitude(value: String) {
        locationTouched = true
        mutateFields { it.copy(longitude = value) }
    }

    // ---- saved places (for example schools) -------------------------------------------

    fun setPlaceName(value: String) {
        mutateFields { it.copy(placeName = value) }
        val matches = PlaceSearch.search(value, places).filterNot { it.name.equals(value.trim(), ignoreCase = true) }
        _state.update { it.copy(placeSuggestions = matches, placeMessage = null) }
    }

    fun dismissPlaceSuggestions() = _state.update { it.copy(placeSuggestions = emptyList()) }

    /** Fills in the name and coordinates of a saved place. The address is looked up when it is not known yet. */
    fun applyPlace(place: SavedPlace) {
        locationTouched = true
        addressTouched = true
        fields = fields.copy(
            placeName = place.name,
            latitude = StampEditing.formatCoordinate(place.latitude),
            longitude = StampEditing.formatCoordinate(place.longitude),
            address = place.address.orEmpty()
        )
        _state.update { it.copy(placeSuggestions = emptyList(), placeMessage = null) }
        recompute()
        if (place.address == null) {
            viewModelScope.launch {
                val found = services.addressResolver.resolve(place.latitude, place.longitude)
                val stillThisPlace = fields.placeName == place.name &&
                    fields.latitude == StampEditing.formatCoordinate(place.latitude) &&
                    fields.address.isBlank()
                if (found != null && stillThisPlace) {
                    fields = fields.copy(address = found)
                    recompute()
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
            _state.update { it.copy(placeMessage = "Type a place name and valid coordinates first.") }
            return
        }
        viewModelScope.launch {
            val existing = places.firstOrNull { it.name.equals(name, ignoreCase = true) }
            val place = (existing ?: SavedPlace(StampFileNames.newId(), name, lat, lon)).copy(
                name = name,
                latitude = lat,
                longitude = lon,
                address = fields.address.trim().ifEmpty { null } ?: existing?.address
            )
            services.placeStore.upsert(place)
            places = services.placeStore.all()
            _state.update { it.copy(placeMessage = "Saved \"$name\" in your places.") }
        }
    }

    fun retryMap() {
        mapKey = null
        scheduleRender()
    }

    fun resetDateTime() {
        val captured = StampEditing.fieldsFrom(capturedContent)
        mutateFields { it.copy(date = captured.date, time = captured.time, zone = captured.zone) }
    }

    fun useGpsFix() {
        val fix = gpsFix ?: return
        locationTouched = false
        addressTouched = false
        originalContent = originalContent.copy(
            latitude = fix.latitude,
            longitude = fix.longitude,
            address = resolvedAddress,
            locationSource = LocationSource.GPS
        )
        mutateFields {
            it.copy(
                latitude = StampEditing.formatCoordinate(fix.latitude),
                longitude = StampEditing.formatCoordinate(fix.longitude),
                address = resolvedAddress.orEmpty()
            )
        }
    }

    // ---- location picked with Google Maps (link, shared text or coordinates) -------------

    fun showMapMessage(text: String) = _state.update { it.copy(mapMessage = text) }

    fun useMapText(text: String) {
        if (_state.value.mapBusy) return
        _state.update { it.copy(mapBusy = true, mapMessage = null) }
        viewModelScope.launch {
            try {
                when (val lookup = services.mapLinks.resolve(text)) {
                    is MapLookup.Failed -> _state.update { it.copy(mapBusy = false, mapMessage = lookup.message) }
                    is MapLookup.Found -> applyMapLocation(lookup.location, lookup.usedInternet)
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _state.update { it.copy(mapBusy = false, mapMessage = "The location could not be read: ${t.message}") }
            }
        }
    }

    private suspend fun applyMapLocation(location: MapLocation, usedInternet: Boolean) {
        locationTouched = true
        addressTouched = true
        // Clear the old address first so it can never sit next to coordinates it does not belong to.
        fields = fields.copy(
            latitude = StampEditing.formatCoordinate(location.latitude),
            longitude = StampEditing.formatCoordinate(location.longitude),
            address = ""
        )
        recompute()

        val geocoded = services.addressResolver.resolve(location.latitude, location.longitude)
        val address = geocoded ?: location.name
        if (address != null) {
            fields = fields.copy(address = address)
            recompute()
        }
        val addressNote = when {
            geocoded != null -> " The address was filled in."
            location.name != null -> " The place name from the link was used as the address."
            else -> " No address was found. Type one if you want."
        }
        val internetNote = if (usedInternet) " One internet request was used to read the short link." else ""
        _state.update {
            it.copy(
                mapBusy = false,
                mapMessage = "Location set from the map. It is marked as typed by you, not GPS.$addressNote$internetNote"
            )
        }
    }

    fun rotate(deltaDegrees: Int) {
        rotation = (((rotation + deltaDegrees) % 360) + 360) % 360
        _state.update { it.copy(rotation = rotation) }
        loadBase()
    }

    fun updateConfig(transform: (StampConfig) -> StampConfig) {
        _state.update { it.copy(config = transform(it.config)) }
        scheduleRender()
    }

    private fun mutateFields(block: (StampFields) -> StampFields) {
        fields = block(fields)
        recompute()
    }

    private fun recompute() {
        val result = StampEditing.apply(fields, originalContent)
        currentContent = result.content
        _state.update {
            it.copy(
                fields = fields,
                errors = result.errors,
                source = result.content.locationSource,
                canUseGps = gpsFix != null && result.content.locationSource != LocationSource.GPS,
                canSavePlace = result.content.placeName != null && StampFormatter.hasValidCoordinates(result.content)
            )
        }
        scheduleRender()
    }

    // ---- location ----------------------------------------------------------------------

    private suspend fun onLocationResult(result: LocationResult) {
        when (result) {
            is LocationResult.Fix -> {
                gpsFix = result
                if (!locationTouched) {
                    originalContent = originalContent.copy(
                        latitude = result.latitude,
                        longitude = result.longitude,
                        locationSource = LocationSource.GPS
                    )
                    fields = fields.copy(
                        latitude = StampEditing.formatCoordinate(result.latitude),
                        longitude = StampEditing.formatCoordinate(result.longitude)
                    )
                }
                _state.update { it.copy(gpsLookup = GpsLookup.FOUND, gpsNote = describeFix(result)) }
                recompute()

                if (!locationTouched) {
                    val address = services.addressResolver.resolve(result.latitude, result.longitude)
                    if (address != null) {
                        resolvedAddress = address
                        if (!addressTouched && !locationTouched) {
                            originalContent = originalContent.copy(address = address)
                            fields = fields.copy(address = address)
                            recompute()
                        }
                    }
                }
            }
            is LocationResult.Unavailable -> {
                _state.update { it.copy(gpsLookup = GpsLookup.UNAVAILABLE, gpsNote = describeReason(result.reason)) }
            }
        }
    }

    private fun describeFix(fix: LocationResult.Fix): String {
        val accuracy = fix.accuracyMeters?.let { " (about \u00B1${it.toInt()} m)" }.orEmpty()
        return if (fix.approximate) "Approximate location found$accuracy" else "GPS location found$accuracy"
    }

    private fun describeReason(reason: LocationResult.Reason): String = when (reason) {
        LocationResult.Reason.PERMISSION_DENIED -> "Location permission is off, so no location was added. Type one below if you want."
        LocationResult.Reason.LOCATION_OFF -> "Location is switched off on this phone, so none was added. Type one below if you want."
        LocationResult.Reason.NO_FIX -> "No location fix was found in time (it helps to be outdoors). None was added."
        LocationResult.Reason.ERROR -> "The location could not be read. None was added."
    }

    private fun describeCaptured(content: StampContent): String {
        val formatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm:ss VV", Locale.getDefault())
        val kind = if (session.existing != null) "Stamp time when last saved " else "Photo taken "
        return kind + formatter.format(content.capturedAt.atZone(content.zoneId))
    }

    // ---- preview + save ----------------------------------------------------------------

    private fun wantsMap(config: StampConfig, content: StampContent) =
        config.layoutStyle == StampLayoutStyle.CARD && config.showMap && StampFormatter.hasValidCoordinates(content)

    private fun scheduleRender() {
        ensureMap()
        doRender()
    }

    /** Downloads the small map picture once per place (and remembers a failure so it does not retry endlessly). */
    private fun ensureMap() {
        val content = currentContent
        val config = _state.value.config
        if (!wantsMap(config, content)) {
            _state.update { it.copy(mapNote = null, mapFailed = false) }
            return
        }
        val lat = content.latitude ?: return
        val lon = content.longitude ?: return
        val key = String.format(Locale.US, "%.5f,%.5f", lat, lon)
        if (key == mapKey) return
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
                        mapNote = if (bitmap == null) "The map picture could not be downloaded, so the card has no map." else null,
                        mapFailed = bitmap == null
                    )
                }
                doRender()
            }
        }
    }

    private fun doRender() {
        val base = baseBitmap ?: return
        val content = currentContent
        val config = _state.value.config
        val map = if (wantsMap(config, content)) mapBitmap else null
        renderJob?.cancel()
        renderJob = viewModelScope.launch(Dispatchers.Default) {
            delay(30)
            val rendered = StampRenderer.renderStamp(base, content, config, map)
            _state.update { it.copy(preview = rendered) }
        }
    }

    fun save(mode: SaveMode = SaveMode.NEW_COPY) {
        val s = _state.value
        if (s.saving || s.loading || s.loadError != null || s.errors.hasErrors) return
        _state.update { it.copy(saving = true, saveError = null) }
        val content = currentContent
        val config = s.config
        viewModelScope.launch {
            try {
                val outcome = services.photoSaver.save(session, content, config, rotation, mode)
                services.stampPrefs.save(config)
                val saved = outcome.saved
                _state.update {
                    it.copy(
                        saving = false,
                        result = SaveResultInfo(
                            saved.uri, saved.relativePath + saved.displayName, saved.displayName,
                            outcome.exifCopied, outcome.replaced, outcome.mapMissing
                        )
                    )
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                val message = if (t is IOException) t.message else "Saving failed: ${t.message}"
                _state.update { it.copy(saving = false, saveError = message ?: "Saving failed.") }
            }
        }
    }

    /**
     * For a new, never-saved photo: removes the app's private working copy.
     * A photo that already exists in the gallery is never touched.
     */
    fun discard() {
        if (_state.value.result != null || session.existing != null) return
        session.location?.cancel()
        runCatching { session.originalFile.delete() }
    }

    private companion object {
        const val PREVIEW_LONG_EDGE = 1280
    }
}
