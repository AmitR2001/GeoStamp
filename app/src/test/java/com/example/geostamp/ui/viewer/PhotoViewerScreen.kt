// geostamp-iteration: 7
package com.example.geostamp.ui.viewer

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.RotateLeft
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.geostamp.domain.model.EditorSession
import com.example.geostamp.domain.model.PhotoRecord
import com.example.geostamp.ui.common.shareImage
import com.example.geostamp.ui.design.PillTone
import com.example.geostamp.ui.design.SecondaryAction
import com.example.geostamp.ui.design.Space
import com.example.geostamp.ui.design.StatusPill
import com.example.geostamp.ui.design.SystemBarIcons
import com.example.geostamp.ui.gallery.GalleryViewModel

@Composable
fun PhotoViewerScreen(
    photoId: String,
    galleryViewModel: GalleryViewModel,
    onClose: () -> Unit,
    onEdit: (EditorSession) -> Unit,
    onMetadata: () -> Unit
) {
    val state by galleryViewModel.state.collectAsState()
    val record = state.photos.firstOrNull { it.id == photoId }
    val snackbar = remember { SnackbarHostState() }

    SystemBarIcons(lightIcons = true)
    BackHandler(onBack = onClose)
    LaunchedEffect(state.message) {
        val text = state.message
        if (text != null) {
            snackbar.showSnackbar(text)
            galleryViewModel.clearMessage()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black, contentColor = Color.White) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (record == null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(Space.xl),
                    verticalArrangement = Arrangement.spacedBy(Space.md, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (state.loading) CircularProgressIndicator(color = Color.White)
                    else Text("This photo is no longer in GeoStamp.", textAlign = TextAlign.Center)
                    TextButton(onClick = onClose) { Text("Back") }
                }
            } else {
                ViewerContent(record, state.revision, galleryViewModel, onClose, onEdit, onMetadata)
            }
            SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp))
        }
    }
}

@Composable
private fun ViewerContent(
    record: PhotoRecord,
    revision: Int,
    vm: GalleryViewModel,
    onClose: () -> Unit,
    onEdit: (EditorSession) -> Unit,
    onMetadata: () -> Unit
) {
    val context = LocalContext.current
    var viewRotation by remember(record.id) { mutableIntStateOf(0) }
    var showDetails by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showRemove by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showAlbum by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    val picture by produceState<Result<Bitmap>?>(null, record.id, record.savedPath, revision) {
        value = vm.loadFull(record)
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { folder ->
        if (folder != null) vm.export(record, folder)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = Space.xs, vertical = Space.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to the gallery")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    record.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (viewRotation != 0) {
                    Text(
                        "View turned. The file is unchanged.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            IconButton(onClick = { showDetails = true }) {
                Icon(Icons.Rounded.Info, contentDescription = "Photo details")
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val result = picture
            when {
                result == null -> CircularProgressIndicator(color = Color.White)
                result.isSuccess -> ZoomableImage(
                    bitmap = result.getOrThrow(),
                    viewRotation = viewRotation,
                    description = "Photo " + record.displayName + ". Pinch to zoom, double tap to reset.",
                    modifier = Modifier.fillMaxSize()
                )
                else -> Column(
                    modifier = Modifier.padding(Space.xl),
                    verticalArrangement = Arrangement.spacedBy(Space.md),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    StatusPill("Can't open this photo", PillTone.Danger)
                    Text(
                        "The saved file may have been moved or deleted in another app. If the original is still kept " +
                            "inside GeoStamp you can stamp it again with Edit.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                    SecondaryAction(text = "Remove from GeoStamp", onClick = { showRemove = true })
                }
            }
        }

        Surface(color = Color(0xFF121212), contentColor = Color.White) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = Space.sm, vertical = Space.xs),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionItem(Icons.Rounded.Edit, "Edit") {
                    val session = vm.sessionFor(record)
                    if (session != null) onEdit(session)
                    else vm.say("The original photo kept inside GeoStamp is missing, so its stamp can't be edited.")
                }
                ActionItem(Icons.Rounded.Share, "Share") {
                    val uri = record.savedUri
                    if (uri == null || !shareImage(context, Uri.parse(uri))) {
                        vm.say("This photo could not be shared.")
                    }
                }
                ActionItem(Icons.Rounded.Tune, "Metadata", onMetadata)
                Box {
                    ActionItem(Icons.Rounded.MoreVert, "More") { menuOpen = true }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Turn view left") },
                            leadingIcon = { Icon(Icons.Rounded.RotateLeft, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                viewRotation = (viewRotation + 270) % 360
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Turn view right") },
                            leadingIcon = { Icon(Icons.Rounded.RotateRight, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                viewRotation = (viewRotation + 90) % 360
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Album") },
                            leadingIcon = { Icon(Icons.Rounded.Folder, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                showAlbum = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            leadingIcon = { Icon(Icons.Rounded.DriveFileRenameOutline, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                showRename = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export to a folder") },
                            leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                exportLauncher.launch(null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            },
                            onClick = {
                                menuOpen = false
                                showDelete = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDetails) DetailsDialog(record, vm) { showDetails = false }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete this photo?") },
            text = {
                Text(
                    "This deletes the saved photo from your phone's gallery and the original copy kept inside " +
                        "GeoStamp. It cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    vm.delete(record, onClose)
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } }
        )
    }

    if (showRemove) {
        AlertDialog(
            onDismissRequest = { showRemove = false },
            title = { Text("Remove from GeoStamp?") },
            text = {
                Text(
                    "This forgets the photo and deletes the original copy kept inside GeoStamp. " +
                        "Nothing in your gallery is touched."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRemove = false
                    vm.removeFromList(record, onClose)
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { showRemove = false }) { Text("Cancel") } }
        )
    }

    if (showRename) RenameDialog(record, onDismiss = { showRename = false }) { name ->
        showRename = false
        vm.rename(record, name)
    }

    if (showAlbum) AlbumDialog(record, vm, onDismiss = { showAlbum = false })
}

@Composable
private fun ActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClickLabel = label, onClick = onClick)
            .padding(horizontal = Space.lg, vertical = Space.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ZoomableImage(bitmap: Bitmap, viewRotation: Int, description: String, modifier: Modifier = Modifier) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { boxSize = it }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 6f)
                    val maxX = boxSize.width * (newScale - 1f) / 2f
                    val maxY = boxSize.height * (newScale - 1f) / 2f
                    offset = if (newScale <= 1f) {
                        Offset.Zero
                    } else {
                        Offset(
                            (offset.x + pan.x).coerceIn(-maxX, maxX),
                            (offset.y + pan.y).coerceIn(-maxY, maxY)
                        )
                    }
                    scale = newScale
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    scale = 1f
                    offset = Offset.Zero
                })
            }
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = description,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                    rotationZ = viewRotation.toFloat()
                }
        )
    }
}

@Composable
private fun DetailsDialog(record: PhotoRecord, vm: GalleryViewModel, onDismiss: () -> Unit) {
    val details by produceState<com.example.geostamp.ui.gallery.PhotoDetails?>(null, record.id, record.savedPath) {
        value = vm.details(record)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Photo details") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val d = details
                if (d == null) {
                    CircularProgressIndicator()
                } else {
                    Text("Name: " + d.name)
                    Text("Saved in: " + d.location)
                    Text("Size: " + d.size + " (" + d.dimensions + ")")
                    Text("Origin: " + d.source)
                    Text("Album: " + d.album)
                    Text(d.stampSource)
                    if (d.stampLines.isNotEmpty()) {
                        Text("Stamp text:", style = MaterialTheme.typography.titleSmall)
                        d.stampLines.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                    Text(
                        "The stamp is drawn on the picture. It is separate from the photo's own camera metadata (EXIF).",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun RenameDialog(record: PhotoRecord, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(record.displayName.removeSuffix(".jpg")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename photo") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("File name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("Rename") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AlbumDialog(record: PhotoRecord, vm: GalleryViewModel, onDismiss: () -> Unit) {
    val state by vm.state.collectAsState()
    var newAlbum by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Album") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Albums are folders inside Pictures/GeoStamp. Choose one or type a new name.",
                    style = MaterialTheme.typography.bodySmall
                )
                state.albums.forEach { album ->
                    TextButton(onClick = {
                        vm.setAlbum(record, album)
                        onDismiss()
                    }) { Text(album) }
                }
                if (record.album != null) {
                    TextButton(onClick = {
                        vm.setAlbum(record, null)
                        onDismiss()
                    }) { Text("Take out of its album") }
                }
                OutlinedTextField(
                    value = newAlbum,
                    onValueChange = { newAlbum = it },
                    label = { Text("New album name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    vm.setAlbum(record, newAlbum)
                    onDismiss()
                },
                enabled = newAlbum.isNotBlank()
            ) { Text("Move to new album") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
