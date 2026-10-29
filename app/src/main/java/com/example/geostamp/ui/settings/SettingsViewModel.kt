// geostamp-iteration: 7
package com.example.geostamp.ui.settings

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.geostamp.app.AppServices
import com.example.geostamp.data.render.StampRenderer
import com.example.geostamp.domain.cleanup.WorkingCopyRules
import com.example.geostamp.domain.maps.MapLookup
import com.example.geostamp.domain.metadata.FileSizes
import com.example.geostamp.domain.places.PlaceSearch
import com.example.geostamp.domain.places.SavedPlace
import com.example.geostamp.domain.stamp.LocationSource
import com.example.geostamp.domain.stamp.StampConfig
import com.example.geostamp.domain.stamp.StampContent
import com.example.geostamp.domain.stamp.StampEditing
import com.example.geostamp.domain.stamp.StampFileNames
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

data class SettingsState(
    val sample: Bitmap? = null,
    val leftoverCount: Int = 0,
    val leftoverSize: String = "0 B",
    val cleaning: Boolean = false,
    val message: String? = null,
    val places: List<SavedPlace> = emptyList()
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val services = AppServices.get(app)
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val sample = withContext(Dispatchers.Default) { renderSample(services.stampPrefs.load()) }
            val scan = services.workingCopies.scan(WorkingCopyRules.MANUAL_MIN_AGE_MS)
            val places = services.placeStore.all()
            _state.update {
                it.copy(
                    sample = sample, leftoverCount = scan.fileCount,
                    leftoverSize = FileSizes.human(scan.totalBytes), places = places
                )
            }
        }
    }

    fun resetStyle() {
        services.stampPrefs.save(StampConfig())
        _state.update { it.copy(message = "The stamp style was reset to the defaults.") }
        refresh()
    }

    fun deletePlace(place: SavedPlace) {
        viewModelScope.launch {
            services.placeStore.delete(place.id)
            _state.update { it.copy(places = services.placeStore.all(), message = "Removed \"${place.name}\" from your places.") }
        }
    }

    /** Checks what was typed in the add-place dialog; returns true when the place was saved. */
    suspend fun savePlace(name: String, latText: String, lonText: String, tagsText: String): Boolean {
        val cleanName = name.trim()
        val lat = StampEditing.parseLatitude(latText)
        val lon = StampEditing.parseLongitude(lonText)
        if (cleanName.isEmpty() || lat == null || lon == null) {
            _state.update { it.copy(message = "A place needs a name and valid latitude and longitude.") }
            return false
        }
        services.placeStore.upsert(
            SavedPlace(StampFileNames.newId(), cleanName, lat, lon, null, PlaceSearch.parseTags(tagsText))
        )
        _state.update { it.copy(places = services.placeStore.all(), message = "Saved \"$cleanName\".") }
        return true
    }

    /** Reads coordinates (and a name, when the link has one) from pasted Maps text for the add-place dialog. */
    fun lookupLink(text: String, onFound: (String, String, String?) -> Unit) {
        viewModelScope.launch {
            try {
                when (val lookup = services.mapLinks.resolve(text)) {
                    is MapLookup.Found -> onFound(
                        StampEditing.formatCoordinate(lookup.location.latitude),
                        StampEditing.formatCoordinate(lookup.location.longitude),
                        lookup.location.name
                    )
                    is MapLookup.Failed -> _state.update { it.copy(message = lookup.message) }
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _state.update { it.copy(message = "The link could not be read: ${t.message}") }
            }
        }
    }

    fun cleanUp() {
        if (_state.value.cleaning) return
        _state.update { it.copy(cleaning = true, message = null) }
        viewModelScope.launch {
            try {
                val removed = services.workingCopies.clean(WorkingCopyRules.MANUAL_MIN_AGE_MS)
                _state.update {
                    it.copy(
                        cleaning = false,
                        message = if (removed == 0) "Nothing to clean up." else "Removed $removed leftover file(s)."
                    )
                }
                refresh()
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _state.update { it.copy(cleaning = false, message = "Clean-up failed: ${t.message}") }
            }
        }
    }

    private fun renderSample(config: StampConfig): Bitmap {
        val base = Bitmap.createBitmap(1200, 800, Bitmap.Config.ARGB_8888)
        val paint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 1200f, 800f,
                Color.rgb(70, 130, 180), Color.rgb(240, 200, 120), Shader.TileMode.CLAMP
            )
        }
        Canvas(base).drawRect(0f, 0f, 1200f, 800f, paint)
        val sample = StampContent(
            address = "Example Street, Example City",
            latitude = 12.9716,
            longitude = 77.5946,
            locationSource = LocationSource.MANUAL,
            capturedAt = Instant.now(),
            zoneId = ZoneId.systemDefault(),
            label = "Sample preview"
        )
        return StampRenderer.renderStamp(base, sample, config, null)
    }
}
