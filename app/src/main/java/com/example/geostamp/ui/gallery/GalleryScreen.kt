// geostamp-iteration: 8
package com.example.geostamp.ui.gallery

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.geostamp.domain.model.EditorSession
import com.example.geostamp.domain.model.PhotoRecord
import com.example.geostamp.domain.stamp.LocationSource
import com.example.geostamp.ui.common.shareImages
import com.example.geostamp.ui.design.Banner
import com.example.geostamp.ui.design.ChipRow
import com.example.geostamp.ui.design.EmptyState
import com.example.geostamp.ui.design.LabeledField
import com.example.geostamp.ui.design.PillTone
import com.example.geostamp.ui.design.PrimaryAction
import com.example.geostamp.ui.design.Space

@Composable
fun GalleryScreen(
    galleryViewModel: GalleryViewModel,
    onOpen: (String) -> Unit,
    onSession: (EditorSession) -> Unit,
    onBatch: (List<EditorSession>) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by galleryViewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val selecting = state.selection.isActive
    var confirmDelete by remember { mutableStateOf(false) }
    var showAlbum by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(30)) { uris ->
        if (uris.isNotEmpty()) {
            galleryViewModel.importPhotos(uris) { sessions ->
                if (sessions.size == 1) onSession(sessions.first()) else onBatch(sessions)
            }
        }
    }
    val launchPicker = {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { folder ->
        if (folder != null) galleryViewModel.exportSelected(folder)
    }

    BackHandler(enabled = selecting) { galleryViewModel.clearSelection() }

    LaunchedEffect(state.message) {
        val text = state.message
        if (text != null) {
            snackbar.showSnackbar(text)
            galleryViewModel.clearMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (selecting) {
                SelectionHeader(
                    count = state.selection.count,
                    onClear = galleryViewModel::clearSelection,
                    onSelectAll = galleryViewModel::selectAllVisible
                )
            } else {
                Column(modifier = Modifier.padding(start = Space.lg, end = Space.lg, top = Space.lg, bottom = Space.sm)) {
                    Text("Gallery", style = MaterialTheme.typography.headlineMedium)
                    val count = state.visible.size
                    val where = state.selectedAlbum?.let { " in " + it } ?: ""
                    Text(
                        (if (count == 1) "1 photo" else "$count photos") + where + ".  Touch and hold a photo to select several.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val progress = state.progressText
            if (progress != null) {
                Banner(text = progress, tone = PillTone.Neutral, modifier = Modifier.padding(horizontal = Space.lg, vertical = Space.xs))
            }

            val albums = state.albums
            if (albums.isNotEmpty() && !selecting) {
                Box(modifier = Modifier.padding(horizontal = Space.lg, vertical = Space.xs)) {
                    ChipRow {
                        FilterChip(
                            selected = state.selectedAlbum == null,
                            onClick = { galleryViewModel.selectAlbum(null) },
                            label = { Text("All") }
                        )
                        albums.forEach { album ->
                            FilterChip(
                                selected = state.selectedAlbum == album,
                                onClick = { galleryViewModel.selectAlbum(album) },
                                label = { Text(album) }
                            )
                        }
                    }
                }
            }

            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.visible.isEmpty() -> EmptyState(
                    title = if (state.photos.isNotEmpty()) "No photos in this album" else "No photos yet",
                    body = if (state.photos.isNotEmpty())
                        "Choose All to see every photo, or move a photo into this album from its page."
                    else "Take a photo on the Camera tab, or add several from your phone.",
                    modifier = Modifier.fillMaxSize(),
                    action = {
                        PrimaryAction(
                            text = "Import photos",
                            onClick = { launchPicker() },
                            icon = Icons.Rounded.AddPhotoAlternate
                        )
                    }
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(112.dp),
                    contentPadding = PaddingValues(start = Space.lg, end = Space.lg, top = Space.sm, bottom = 104.dp),
                    horizontalArrangement = Arrangement.spacedBy(Space.sm),
                    verticalArrangement = Arrangement.spacedBy(Space.sm),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.visible, key = { it.id }) { record ->
                        ThumbnailCell(
                            record = record,
                            revision = state.revision,
                            vm = galleryViewModel,
                            selected = state.selection.contains(record.id),
                            selecting = selecting,
                            onTap = {
                                if (selecting) galleryViewModel.toggleSelected(record.id) else onOpen(record.id)
                            },
                            onLongPress = { galleryViewModel.toggleSelected(record.id) }
                        )
                    }
                }
            }
        }

        if (!selecting && state.photos.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = { launchPicker() },
                modifier = Modifier.align(Alignment.BottomEnd).padding(Space.lg),
                icon = { Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null) },
                text = { Text(if (state.busy) "Importing..." else "Import photos") }
            )
        }

        if (selecting) {
            SelectionBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                onStamp = {
                    val (sessions, skipped) = galleryViewModel.sessionsForSelected()
                    if (sessions.isEmpty()) {
                        galleryViewModel.say("The originals of these photos are no longer kept inside GeoStamp, so they cannot be stamped again.")
                    } else {
                        if (skipped > 0) galleryViewModel.say("$skipped photo(s) skipped: their originals are missing.")
                        galleryViewModel.clearSelection()
                        if (sessions.size == 1) onSession(sessions.first()) else onBatch(sessions)
                    }
                },
                onShare = {
                    val uris = galleryViewModel.selectedUris()
                    if (!shareImages(context, uris)) galleryViewModel.say("The photos could not be shared.")
                },
                onExport = { exportLauncher.launch(null) },
                onAlbum = { showAlbum = true },
                onDelete = { confirmDelete = true }
            )
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = if (selecting) 96.dp else 88.dp)
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete " + state.selection.count + " photos?") },
            text = {
                Text(
                    "This deletes the saved photos from your phone's gallery and the original copies kept inside " +
                        "GeoStamp. It cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    galleryViewModel.deleteSelected()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }

    if (showAlbum) {
        BulkAlbumDialog(
            albums = state.albums,
            count = state.selection.count,
            onPick = { album ->
                showAlbum = false
                galleryViewModel.moveSelectedToAlbum(album)
            },
            onDismiss = { showAlbum = false }
        )
    }
}

@Composable
private fun SelectionHeader(count: Int, onClear: () -> Unit, onSelectAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.sm, vertical = Space.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClear) { Icon(Icons.Rounded.Close, contentDescription = "Stop selecting") }
        Text(
            "$count selected",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onSelectAll) { Text("Select all") }
    }
}

@Composable
private fun SelectionBar(
    modifier: Modifier,
    onStamp: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit,
    onAlbum: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Space.xs, vertical = Space.xs),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BarAction(Icons.Rounded.Edit, "Stamp", onStamp)
            BarAction(Icons.Rounded.Share, "Share", onShare)
            BarAction(Icons.Rounded.Save, "Export", onExport)
            BarAction(Icons.Rounded.Folder, "Album", onAlbum)
            BarAction(Icons.Rounded.Delete, "Delete", onDelete)
        }
    }
}

@Composable
private fun BarAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun BulkAlbumDialog(albums: List<String>, count: Int, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    var newAlbum by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Album for $count photos") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Space.xs)
            ) {
                Text(
                    "Albums are folders inside Pictures/GeoStamp. Choose one or type a new name.",
                    style = MaterialTheme.typography.bodySmall
                )
                albums.forEach { album -> TextButton(onClick = { onPick(album) }) { Text(album) } }
                TextButton(onClick = { onPick(null) }) { Text("Take out of their album") }
                LabeledField("New album name", newAlbum, { newAlbum = it })
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick(newAlbum) }, enabled = newAlbum.isNotBlank()) { Text("Move to new album") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ThumbnailCell(
    record: PhotoRecord,
    revision: Int,
    vm: GalleryViewModel,
    selected: Boolean,
    selecting: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val thumb by produceState<Pair<Boolean, Bitmap?>>(false to null, record.id, record.savedPath, revision) {
        value = true to vm.thumbnail(record)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(record.id) {
                detectTapGestures(onTap = { onTap() }, onLongPress = { onLongPress() })
            }
            .semantics {
                contentDescription = "Photo " + record.displayName
                this.selected = selected
            },
        contentAlignment = Alignment.Center
    ) {
        val bitmap = thumb.second
        when {
            bitmap != null -> Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            thumb.first -> Text(
                "Can't open",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(Space.xs)
            )
            else -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
        if (selected) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)))
        }
        if (record.content?.locationSource == LocationSource.GPS && !selecting) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Place, contentDescription = "Stamped with a GPS location", tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
        if (selecting) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color(0x66000000))
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
