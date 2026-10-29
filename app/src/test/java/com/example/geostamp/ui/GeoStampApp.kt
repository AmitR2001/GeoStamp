// geostamp-iteration: 7
package com.example.geostamp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.geostamp.app.AppServices
import com.example.geostamp.domain.cleanup.WorkingCopyRules
import com.example.geostamp.domain.model.EditorSession
import com.example.geostamp.ui.camera.CameraScreen
import com.example.geostamp.ui.design.Banner
import com.example.geostamp.ui.design.PillTone
import com.example.geostamp.ui.design.Space
import com.example.geostamp.ui.editor.EditorScreen
import com.example.geostamp.ui.gallery.GalleryScreen
import com.example.geostamp.ui.gallery.GalleryViewModel
import com.example.geostamp.ui.metadata.MetadataScreen
import com.example.geostamp.ui.settings.SettingsScreen
import com.example.geostamp.ui.viewer.PhotoViewerScreen

@Composable
fun GeoStampApp(
    mainViewModel: MainViewModel = viewModel(),
    galleryViewModel: GalleryViewModel = viewModel()
) {
    val session by mainViewModel.session.collectAsState()
    val viewerId by mainViewModel.viewerId.collectAsState()
    val metadataId by mainViewModel.metadataId.collectAsState()
    val sharedText by mainViewModel.sharedText.collectAsState()
    val openSession = session
    val openViewerId = viewerId
    val openMetadataId = metadataId
    val context = LocalContext.current

    // Once per start: remove old private working copies that no saved photo uses.
    LaunchedEffect(Unit) {
        AppServices.get(context).workingCopies.clean(WorkingCopyRules.AUTO_MIN_AGE_MS)
    }

    // Reload the photo list at start and every time the editor or metadata page closes.
    LaunchedEffect(openSession == null && openMetadataId == null) {
        if (openSession == null && openMetadataId == null) galleryViewModel.refresh()
    }

    when {
        openSession != null -> EditorScreen(
            session = openSession,
            sharedText = sharedText,
            onSharedConsumed = mainViewModel::consumeSharedText,
            onClose = mainViewModel::closeEditor
        )
        openMetadataId != null -> MetadataScreen(
            photoId = openMetadataId,
            galleryViewModel = galleryViewModel,
            onClose = mainViewModel::closeMetadata
        )
        openViewerId != null -> PhotoViewerScreen(
            photoId = openViewerId,
            galleryViewModel = galleryViewModel,
            onClose = mainViewModel::closeViewer,
            onEdit = mainViewModel::openEditor,
            onMetadata = { mainViewModel.openMetadata(openViewerId) }
        )
        else -> MainTabs(
            galleryViewModel = galleryViewModel,
            sharedText = sharedText,
            onDismissShared = mainViewModel::consumeSharedText,
            onPhotoCaptured = mainViewModel::openEditor,
            onOpenPhoto = mainViewModel::openViewer
        )
    }
}

@Composable
private fun MainTabs(
    galleryViewModel: GalleryViewModel,
    sharedText: String?,
    onDismissShared: () -> Unit,
    onPhotoCaptured: (EditorSession) -> Unit,
    onOpenPhoto: (String) -> Unit
) {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val destinations = AppDestination.entries
    val current = destinations[selectedIndex]

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                destinations.forEachIndexed { index, destination ->
                    NavigationBarItem(
                        selected = index == selectedIndex,
                        onClick = { selectedIndex = index },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(destination.label) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        // The camera fills the whole screen behind the status bar; other tabs respect all insets.
        val contentModifier = if (current == AppDestination.CAMERA) {
            Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        } else {
            Modifier.padding(innerPadding)
        }
        Column(modifier = contentModifier.fillMaxSize()) {
            if (sharedText != null) {
                Banner(
                    text = "A place was shared from Maps: " + sharedText.lineSequence().firstOrNull().orEmpty().take(60) +
                        ". Open a photo, choose Edit stamp, then tap Use the shared location.",
                    tone = PillTone.Positive,
                    icon = Icons.Rounded.Place,
                    modifier = Modifier.padding(Space.md),
                    action = { TextButton(onClick = onDismissShared) { Text("Dismiss") } }
                )
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (current) {
                    AppDestination.CAMERA -> CameraScreen(onSession = onPhotoCaptured)
                    AppDestination.GALLERY -> GalleryScreen(
                        galleryViewModel = galleryViewModel,
                        onOpen = onOpenPhoto,
                        onSession = onPhotoCaptured
                    )
                    AppDestination.SETTINGS -> SettingsScreen()
                }
            }
        }
    }
}
