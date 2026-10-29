// geostamp-iteration: 7
package com.example.geostamp.ui.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.FlashAuto
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.geostamp.domain.model.EditorSession
import com.example.geostamp.permissions.LocationAccess
import com.example.geostamp.ui.design.Banner
import com.example.geostamp.ui.design.GeoMark
import com.example.geostamp.ui.design.PillTone
import com.example.geostamp.ui.design.PrimaryAction
import com.example.geostamp.ui.design.SecondaryAction
import com.example.geostamp.ui.design.SectionCard
import com.example.geostamp.ui.design.Space
import com.example.geostamp.ui.design.StatusPill
import com.example.geostamp.ui.design.SystemBarIcons
import kotlinx.coroutines.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private fun hasPermission(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private fun currentLocationAccess(context: Context): LocationAccess = LocationAccess.from(
    fineGranted = hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION),
    coarseGranted = hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
)

private fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

private suspend fun awaitCameraProvider(context: Context): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                continuation.resume(future.get())
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

private fun nextFlashMode(mode: Int): Int = when (mode) {
    ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
    ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
    else -> ImageCapture.FLASH_MODE_OFF
}

private fun flashLabel(mode: Int): String = when (mode) {
    ImageCapture.FLASH_MODE_AUTO -> "Flash auto"
    ImageCapture.FLASH_MODE_ON -> "Flash on"
    else -> "Flash off"
}

private fun flashIcon(mode: Int): ImageVector = when (mode) {
    ImageCapture.FLASH_MODE_AUTO -> Icons.Rounded.FlashAuto
    ImageCapture.FLASH_MODE_ON -> Icons.Rounded.FlashOn
    else -> Icons.Rounded.FlashOff
}

@Composable
fun CameraScreen(
    onSession: (EditorSession) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CameraViewModel = viewModel()
) {
    val context = LocalContext.current
    var cameraGranted by remember { mutableStateOf(hasPermission(context, Manifest.permission.CAMERA)) }
    var locationAccess by remember { mutableStateOf(currentLocationAccess(context)) }
    val uiState by viewModel.state.collectAsState()

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> cameraGranted = granted }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> locationAccess = currentLocationAccess(context) }

    val activity = context as? ComponentActivity
    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                cameraGranted = hasPermission(context, Manifest.permission.CAMERA)
                locationAccess = currentLocationAccess(context)
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }

    val requestLocation = {
        locationLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    if (cameraGranted) {
        LiveCamera(
            modifier = modifier,
            locationAccess = locationAccess,
            ui = uiState,
            onRequestLocation = { requestLocation() },
            onCapture = { imageCapture -> viewModel.capture(imageCapture, onSession) },
            onDismissMessage = viewModel::dismissMessage
        )
    } else {
        PermissionPrompt(
            modifier = modifier,
            locationAccess = locationAccess,
            onRequestCamera = { cameraLauncher.launch(Manifest.permission.CAMERA) },
            onRequestLocation = { requestLocation() },
            onOpenSettings = { openAppSettings(context) }
        )
    }
}

@Composable
private fun LiveCamera(
    modifier: Modifier,
    locationAccess: LocationAccess,
    ui: CameraUiState,
    onRequestLocation: () -> Unit,
    onCapture: (ImageCapture) -> Unit,
    onDismissMessage: () -> Unit
) {
    SystemBarIcons(lightIcons = true)
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember {
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
    }
    var lensFacing by rememberSaveable { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by rememberSaveable { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var hasFront by remember { mutableStateOf(false) }
    var hasBack by remember { mutableStateOf(false) }
    var cameraReady by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(lensFacing) {
        try {
            val cameraProvider = awaitCameraProvider(context)
            provider = cameraProvider
            hasBack = cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
            hasFront = cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
            val selector = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(activity, selector, preview, imageCapture)
            cameraReady = true
            cameraError = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            cameraReady = false
            cameraError = "The camera could not start: ${e.message ?: "unknown error"}"
        }
    }

    LaunchedEffect(flashMode) { imageCapture.flashMode = flashMode }

    DisposableEffect(Unit) {
        onDispose { provider?.unbindAll() }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xAA000000), Color.Transparent)))
                .statusBarsPadding()
                .padding(horizontal = Space.lg, vertical = Space.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LocationChip(locationAccess, onRequestLocation)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
                .padding(horizontal = Space.xl)
                .padding(top = Space.xxl, bottom = Space.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            val problem = ui.message ?: cameraError
            if (problem != null) {
                Banner(
                    text = problem,
                    tone = PillTone.Danger,
                    action = if (ui.message != null) {
                        { TextButton(onClick = onDismissMessage) { Text("Dismiss") } }
                    } else {
                        null
                    }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CameraIconButton(flashIcon(flashMode), flashLabel(flashMode) + ". Tap to change.") {
                    flashMode = nextFlashMode(flashMode)
                }
                ShutterButton(enabled = cameraReady && !ui.busy, busy = ui.busy) {
                    imageCapture.targetRotation = previewView.display?.rotation ?: Surface.ROTATION_0
                    onCapture(imageCapture)
                }
                if (hasFront && hasBack) {
                    CameraIconButton(Icons.Rounded.FlipCameraAndroid, "Switch between front and back camera") {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    }
                } else {
                    Spacer(Modifier.size(52.dp))
                }
            }
        }
    }
}

@Composable
private fun LocationChip(access: LocationAccess, onRequest: () -> Unit) {
    when (access) {
        LocationAccess.PRECISE -> StatusPill("Precise location on", PillTone.Positive, icon = Icons.Rounded.LocationOn)
        LocationAccess.APPROXIMATE -> StatusPill("Approximate location on", PillTone.Neutral, icon = Icons.Rounded.LocationOn)
        LocationAccess.NONE -> StatusPill(
            "Location off. Tap to allow",
            PillTone.Caution,
            modifier = Modifier.clickable(onClickLabel = "Allow location", role = Role.Button, onClick = onRequest),
            icon = Icons.Rounded.LocationOn
        )
    }
}

@Composable
private fun CameraIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(52.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color(0x66000000),
            contentColor = Color.White
        )
    ) {
        Icon(icon, contentDescription = description)
    }
}

@Composable
private fun ShutterButton(enabled: Boolean, busy: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(84.dp)
            .clip(CircleShape)
            .border(4.dp, Color.White, CircleShape)
            .clickable(enabled = enabled, onClickLabel = "Take photo", role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Take photo" },
        contentAlignment = Alignment.Center
    ) {
        if (busy) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp), color = Color.White, strokeWidth = 3.dp)
        } else {
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .clip(CircleShape)
                    .background(if (enabled) Color.White else Color.White.copy(alpha = 0.4f))
            )
        }
    }
}

@Composable
private fun PermissionPrompt(
    modifier: Modifier,
    locationAccess: LocationAccess,
    onRequestCamera: () -> Unit,
    onRequestLocation: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.xl, vertical = Space.xl),
        verticalArrangement = Arrangement.spacedBy(Space.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(Space.lg))
        GeoMark(markSize = 120.dp)
        Text(
            "Photos that remember where they were taken",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Text(
            "GeoStamp writes the place, date and time onto your pictures. Everything stays on your phone.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        PrimaryAction(
            text = "Allow camera",
            onClick = onRequestCamera,
            icon = Icons.Rounded.CameraAlt,
            modifier = Modifier.fillMaxWidth()
        )
        SectionCard(title = "Location (optional)", icon = Icons.Rounded.LocationOn) {
            Text(
                "Used only to write coordinates and an address on a stamp when you take a photo. " +
                    "If you decline, you can type the place yourself.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            when (locationAccess) {
                LocationAccess.PRECISE -> StatusPill("Precise location allowed", PillTone.Positive)
                LocationAccess.APPROXIMATE -> StatusPill("Approximate location allowed", PillTone.Neutral)
                LocationAccess.NONE -> StatusPill("Not allowed yet", PillTone.Neutral)
            }
            if (locationAccess != LocationAccess.PRECISE) {
                SecondaryAction(text = "Allow location", onClick = onRequestLocation, icon = Icons.Rounded.LocationOn)
            }
        }
        TextButton(onClick = onOpenSettings) { Text("Open app settings") }
    }
}
