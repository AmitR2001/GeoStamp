// geostamp-iteration: 8
package com.example.geostamp.ui

import androidx.lifecycle.ViewModel
import com.example.geostamp.domain.model.EditorSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Holds which full-screen page is open and any place shared from Maps, so they survive rotation. */
class MainViewModel : ViewModel() {
    private val _session = MutableStateFlow<EditorSession?>(null)
    val session: StateFlow<EditorSession?> = _session.asStateFlow()

    private val _viewerId = MutableStateFlow<String?>(null)
    val viewerId: StateFlow<String?> = _viewerId.asStateFlow()

    private val _metadataId = MutableStateFlow<String?>(null)
    val metadataId: StateFlow<String?> = _metadataId.asStateFlow()

    private val _sharedText = MutableStateFlow<String?>(null)
    val sharedText: StateFlow<String?> = _sharedText.asStateFlow()

    private val _batch = MutableStateFlow<List<EditorSession>?>(null)
    val batch: StateFlow<List<EditorSession>?> = _batch.asStateFlow()

    /** Several photos opened together on the "stamp many photos" page. */
    fun openBatch(sessions: List<EditorSession>) { _batch.value = sessions }
    fun closeBatch() { _batch.value = null }

    fun openEditor(session: EditorSession) { _session.value = session }
    fun closeEditor() { _session.value = null }
    fun openViewer(photoId: String) { _viewerId.value = photoId }
    fun closeViewer() { _viewerId.value = null }
    fun openMetadata(photoId: String) { _metadataId.value = photoId }
    fun closeMetadata() { _metadataId.value = null }

    /** Text shared into GeoStamp from another app (for example a place from Google Maps). */
    fun receiveSharedText(text: String) { _sharedText.value = text }
    fun consumeSharedText() { _sharedText.value = null }
}
