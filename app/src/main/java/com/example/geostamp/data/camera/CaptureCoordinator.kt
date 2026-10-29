package com.example.geostamp.data.camera

import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import com.example.geostamp.data.location.LocationProvider
import com.example.geostamp.domain.model.EditorSession
import com.example.geostamp.domain.model.PhotoSource
import com.example.geostamp.domain.stamp.StampFileNames
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.time.Instant
import java.time.ZoneId
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Takes the picture and, in parallel, looks up the location. The editor opens as soon as the
 * photo is on disk; the location result arrives later through [EditorSession.location].
 */
class CaptureCoordinator(
    private val context: Context,
    private val locationProvider: LocationProvider,
    private val originalsDir: File
) {
    suspend fun capture(imageCapture: ImageCapture, scope: CoroutineScope): EditorSession {
        val id = StampFileNames.newId()
        originalsDir.mkdirs()
        val file = File(originalsDir, "$id.jpg")
        val location = if (locationProvider.hasPermission()) {
            scope.async { locationProvider.currentLocation() }
        } else {
            null
        }
        val capturedAt = Instant.now()
        try {
            takePicture(imageCapture, file)
        } catch (t: Throwable) {
            location?.cancel()
            file.delete()
            throw t
        }
        return EditorSession(id, file, capturedAt, ZoneId.systemDefault(), location, PhotoSource.CAMERA)
    }

    private suspend fun takePicture(imageCapture: ImageCapture, file: File) =
        suspendCancellableCoroutine<Unit> { cont ->
            val options = ImageCapture.OutputFileOptions.Builder(file).build()
            imageCapture.takePicture(
                options,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        cont.resume(Unit)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        cont.resumeWithException(exception)
                    }
                }
            )
        }
}
