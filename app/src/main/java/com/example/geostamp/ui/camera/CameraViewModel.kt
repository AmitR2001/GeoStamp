package com.example.geostamp.ui.camera

import android.app.Application
import androidx.camera.core.ImageCapture
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.geostamp.app.AppServices
import com.example.geostamp.domain.model.EditorSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CameraUiState(val busy: Boolean = false, val message: String? = null)

class CameraViewModel(app: Application) : AndroidViewModel(app) {
    private val services = AppServices.get(app)
    private val _state = MutableStateFlow(CameraUiState())
    val state: StateFlow<CameraUiState> = _state.asStateFlow()

    fun capture(imageCapture: ImageCapture, onSession: (EditorSession) -> Unit) {
        if (_state.value.busy) return
        _state.value = CameraUiState(busy = true)
        viewModelScope.launch {
            try {
                val session = services.captureCoordinator.capture(imageCapture, viewModelScope)
                _state.value = CameraUiState()
                onSession(session)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = CameraUiState(message = "The photo could not be taken: ${e.message ?: "unknown error"}")
            }
        }
    }

    fun dismissMessage() {
        _state.value = _state.value.copy(message = null)
    }
}
