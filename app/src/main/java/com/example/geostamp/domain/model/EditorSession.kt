// geostamp-iteration: 5
package com.example.geostamp.domain.model

import com.example.geostamp.domain.location.LocationResult
import com.example.geostamp.domain.stamp.StampConfig
import com.example.geostamp.domain.stamp.StampContent
import kotlinx.coroutines.Deferred
import java.io.File
import java.time.Instant
import java.time.ZoneId

/**
 * A photo that is open in the stamp editor.
 *
 * - New camera photo: [location] is still being looked up when the editor opens.
 * - Imported photo: [initialContent] may carry the date and GPS found in the photo's own metadata.
 * - Existing saved photo: [existing] is set, with its saved stamp in [initialContent]/[initialConfig].
 */
class EditorSession(
    val id: String,
    val originalFile: File,
    val capturedAt: Instant,
    val zoneId: ZoneId,
    val location: Deferred<LocationResult>?,
    val source: PhotoSource,
    val initialContent: StampContent? = null,
    val initialConfig: StampConfig? = null,
    val existing: PhotoRecord? = null,
    val rotationDegrees: Int = 0,
    val infoNote: String? = null
)
