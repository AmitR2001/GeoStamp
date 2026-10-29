// geostamp-iteration: 7
package com.example.geostamp.ui.gallery

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.geostamp.domain.model.EditorSession
import com.example.geostamp.domain.model.PhotoRecord
import com.example.geostamp.domain.stamp.LocationSource
import com.example.geostamp.ui.design.ChipRow
import com.example.geostamp.ui.design.EmptyState
import com.example.geostamp.ui.design.PrimaryAction
import com.example.geostamp.ui.design.Space

@Composable
fun GalleryScreen(
    galleryViewModel: GalleryViewModel,
    onOpen: (String) -> Unit,
    onSession: (EditorSession) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by galleryViewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) galleryViewModel.importPhoto(uri, onSession)
    }
    val launchPicker = {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    LaunchedEffect(state.message) {
        val text = state.message
        if (text != null) {
            snackbar.showSnackbar(text)
            galleryViewModel.clearMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(start = Space.lg, end = Space.lg, top = Space.lg, bottom = Space.sm)) {
                Text("Gallery", style = MaterialTheme.typography.headlineMedium)
                val count = state.visible.size
                val where = state.selectedAlbum?.let { " in " + it } ?: ""
                Text(
                    (if (count == 1) "1 photo" else "$count photos") + where,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val albums = state.albums
            if (albums.isNotEmpty()) {
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
                    else "Take a photo on the Camera tab, or add one from your phone.",
                    modifier = Modifier.fillMaxSize(),
                    action = {
                        PrimaryAction(
                            text = "Import a photo",
                            onClick = { launchPicker() },
                            icon = Icons.Rounded.AddPhotoAlternate
                        )
                    }
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(112.dp),
                    contentPadding = PaddingValues(start = Space.lg, end = Space.lg, top = Space.sm, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(Space.sm),
                    verticalArrangement = Arrangement.spacedBy(Space.sm),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.visible, key = { it.id }) { record ->
                        ThumbnailCell(record, state.revision, galleryViewModel) { onOpen(record.id) }
                    }
                }
            }
        }

        if (state.photos.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = { launchPicker() },
                modifier = Modifier.align(Alignment.BottomEnd).padding(Space.lg),
                icon = { Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null) },
                text = { Text(if (state.busy) "Importing..." else "Import photo") }
            )
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp)
        )
    }
}

@Composable
private fun ThumbnailCell(record: PhotoRecord, revision: Int, vm: GalleryViewModel, onClick: () -> Unit) {
    val thumb by produceState<Pair<Boolean, Bitmap?>>(false to null, record.id, record.savedPath, revision) {
        value = true to vm.thumbnail(record)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Photo " + record.displayName },
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
        if (record.content?.locationSource == LocationSource.GPS) {
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
    }
}
