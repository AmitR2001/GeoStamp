// geostamp-iteration: 7
package com.example.geostamp.ui.metadata

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.geostamp.ui.design.Banner
import com.example.geostamp.ui.design.LabeledField
import com.example.geostamp.ui.design.PillTone
import com.example.geostamp.ui.design.PrimaryAction
import com.example.geostamp.ui.design.SecondaryAction
import com.example.geostamp.ui.design.SectionCard
import com.example.geostamp.ui.design.Space
import com.example.geostamp.ui.design.ToggleRow
import com.example.geostamp.ui.gallery.GalleryViewModel

@Composable
fun MetadataScreen(
    photoId: String,
    galleryViewModel: GalleryViewModel,
    onClose: () -> Unit,
    viewModel: MetadataViewModel = viewModel(key = photoId)
) {
    val gallery by galleryViewModel.state.collectAsState()
    val record = gallery.photos.firstOrNull { it.id == photoId }
    val state by viewModel.state.collectAsState()

    BackHandler(onBack = onClose)
    LaunchedEffect(record?.id) {
        if (record != null) viewModel.start(record)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = Space.sm, vertical = Space.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to the photo")
                }
                Column {
                    Text("Photo metadata", style = MaterialTheme.typography.titleLarge)
                    Text(
                        record?.displayName.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Space.lg, vertical = Space.md),
                verticalArrangement = Arrangement.spacedBy(Space.lg)
            ) {
                if (record == null) {
                    Text("This photo is no longer in GeoStamp.")
                } else {
                    InspectorCard(state)
                    EditorCard(state, viewModel)
                }
            }
        }
    }
}

@Composable
private fun InspectorCard(state: MetadataState) {
    SectionCard(title = "Stored inside the file", icon = Icons.Rounded.Info) {
        Text(
            "This is the photo's hidden metadata (EXIF), read from the saved file. " +
                "It is separate from the stamp drawn on the picture.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (state.loading) {
            CircularProgressIndicator()
        } else {
            state.entries.forEach { entry ->
                Column {
                    Text(entry.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(entry.value, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun EditorCard(state: MetadataState, vm: MetadataViewModel) {
    SectionCard(title = "Change the metadata (optional)", icon = Icons.Rounded.Edit) {
        Banner(
            text = "Changing metadata can make a photo claim something that is not true, so only enter data you know " +
                "is correct. GeoStamp always writes changes into a new copy; the existing file is never changed.",
            tone = PillTone.Caution
        )

        ToggleRow("Change the location in the metadata", state.changeLocation, vm::setChangeLocation)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            LabeledField(
                "Latitude", state.latitude, vm::setLatitude,
                modifier = Modifier.weight(1f), error = state.errors.latitude, enabled = state.changeLocation, mono = true
            )
            LabeledField(
                "Longitude", state.longitude, vm::setLongitude,
                modifier = Modifier.weight(1f), error = state.errors.longitude, enabled = state.changeLocation, mono = true
            )
        }

        ToggleRow("Change the date taken in the metadata", state.changeDate, vm::setChangeDate)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            LabeledField(
                "Date", state.date, vm::setDate,
                modifier = Modifier.weight(1f), error = state.errors.date, enabled = state.changeDate, mono = true
            )
            LabeledField(
                "Time", state.time, vm::setTime,
                modifier = Modifier.weight(1f), error = state.errors.time, enabled = state.changeDate, mono = true
            )
        }
        LabeledField("Time zone", state.zone, vm::setZone, error = state.errors.zone, enabled = state.changeDate, mono = true)

        PrimaryAction(
            text = if (state.busy) "Saving..." else "Save the changes as a new copy",
            onClick = vm::saveChanges,
            enabled = vm.canSave(state),
            modifier = Modifier.fillMaxWidth()
        )
        SecondaryAction(
            text = "Remove all location data in a new copy",
            onClick = vm::removeLocationCopy,
            enabled = !state.busy && !state.loading,
            modifier = Modifier.fillMaxWidth()
        )

        if (state.message != null) {
            Text(state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        if (state.savedPath != null) {
            Banner(text = "Saved a new copy: " + state.savedPath, tone = PillTone.Positive)
        }
    }
}
