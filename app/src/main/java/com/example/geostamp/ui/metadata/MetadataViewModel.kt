package com.example.geostamp.ui.metadata

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.geostamp.app.AppServices
import com.example.geostamp.domain.metadata.ExifChanges
import com.example.geostamp.domain.metadata.ExifDates
import com.example.geostamp.domain.metadata.ExifEditErrors
import com.example.geostamp.domain.metadata.ExifEditing
import com.example.geostamp.domain.metadata.ExifEntry
import com.example.geostamp.domain.model.PhotoRecord
import com.example.geostamp.domain.stamp.StampContent
import com.example.geostamp.domain.stamp.StampEditing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

data class MetadataState(
    val loading: Boolean = true,
    val entries: List<ExifEntry> = emptyList(),
    val hasGps: Boolean = false,
    val changeLocation: Boolean = false,
    val latitude: String = "",
    val longitude: String = "",
    val changeDate: Boolean = false,
    val date: String = "",
    val time: String = "",
    val zone: String = "",
    val errors: ExifEditErrors = ExifEditErrors(),
    val busy: Boolean = false,
    val message: String? = null,
    val savedPath: String? = null
)

class MetadataViewModel(app: Application) : AndroidViewModel(app) {
    private val services = AppServices.get(app)
    private val _state = MutableStateFlow(MetadataState())
    val state: StateFlow<MetadataState> = _state.asStateFlow()

    private var startedId: String? = null
    private lateinit var record: PhotoRecord

    fun start(record: PhotoRecord) {
        if (startedId == record.id) return
        startedId = record.id
        this.record = record
        viewModelScope.launch {
            try {
                val snapshot = services.metadata.read(record)
                val exifTime = ExifDates.parse(snapshot.dateTimeOriginal, null, ZoneId.systemDefault())
                val fields = StampEditing.fieldsFrom(
                    StampContent(
                        capturedAt = exifTime?.instant ?: Instant.now(),
                        zoneId = exifTime?.zone ?: ZoneId.systemDefault()
                    )
                )
                _state.update {
                    it.copy(
                        loading = false,
                        entries = snapshot.entries,
                        hasGps = snapshot.latitude != null,
                        latitude = snapshot.latitude?.let { v -> StampEditing.formatCoordinate(v) }.orEmpty(),
                        longitude = snapshot.longitude?.let { v -> StampEditing.formatCoordinate(v) }.orEmpty(),
                        date = fields.date,
                        time = fields.time,
                        zone = fields.zone
                    )
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _state.update { it.copy(loading = false, message = t.message ?: "The metadata could not be read.") }
            }
        }
    }

    fun setChangeLocation(on: Boolean) = edit { it.copy(changeLocation = on) }
    fun setChangeDate(on: Boolean) = edit { it.copy(changeDate = on) }
    fun setLatitude(v: String) = edit { it.copy(latitude = v) }
    fun setLongitude(v: String) = edit { it.copy(longitude = v) }
    fun setDate(v: String) = edit { it.copy(date = v) }
    fun setTime(v: String) = edit { it.copy(time = v) }
    fun setZone(v: String) = edit { it.copy(zone = v) }

    private fun edit(block: (MetadataState) -> MetadataState) {
        _state.update { current ->
            val next = block(current)
            val result = ExifEditing.validate(
                next.changeLocation, next.latitude, next.longitude,
                next.changeDate, next.date, next.time, next.zone
            )
            next.copy(errors = result.errors, message = null)
        }
    }

    fun canSave(s: MetadataState): Boolean {
        val result = ExifEditing.validate(s.changeLocation, s.latitude, s.longitude, s.changeDate, s.date, s.time, s.zone)
        return result.changes != null && !s.busy && !s.loading
    }

    fun saveChanges() {
        val s = _state.value
        val result = ExifEditing.validate(s.changeLocation, s.latitude, s.longitude, s.changeDate, s.date, s.time, s.zone)
        val changes = result.changes
        if (changes == null) {
            _state.update { it.copy(errors = result.errors, message = "Switch on what you want to change and fix any red fields.") }
            return
        }
        write(changes)
    }

    fun removeLocationCopy() = write(ExifChanges(removeLocation = true))

    private fun write(changes: ExifChanges) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, message = null, savedPath = null) }
        viewModelScope.launch {
            try {
                val updated = services.metadata.saveCopy(record, changes)
                _state.update { it.copy(busy = false, savedPath = updated.savedPath ?: updated.displayName) }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _state.update { it.copy(busy = false, message = t.message ?: "The copy could not be saved.") }
            }
        }
    }
}
