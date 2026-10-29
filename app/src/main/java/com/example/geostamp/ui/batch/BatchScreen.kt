// geostamp-iteration: 8
package com.example.geostamp.ui.batch

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.geostamp.domain.model.EditorSession
import com.example.geostamp.domain.stamp.StampLayoutStyle
import com.example.geostamp.ui.design.Banner
import com.example.geostamp.ui.design.ChipRow
import com.example.geostamp.ui.design.LabeledField
import com.example.geostamp.ui.design.PillTone
import com.example.geostamp.ui.design.PlaceSuggestionList
import com.example.geostamp.ui.design.PrimaryAction
import com.example.geostamp.ui.design.SectionCard
import com.example.geostamp.ui.design.Space
import com.example.geostamp.ui.design.ToggleRow
import java.util.Locale

/** One place, note, date and style for many photos at once. Each photo is saved as its own new copy. */
@Composable
fun BatchScreen(
    sessions: List<EditorSession>,
    onClose: () -> Unit,
    viewModel: BatchViewModel = viewModel(key = sessions.joinToString(",") { it.id }.hashCode().toString())
) {
    val state by viewModel.state.collectAsState()
    var confirmLeave by remember { mutableStateOf(false) }

    LaunchedEffect(sessions) { viewModel.start(sessions) }

    val leave: () -> Unit = {
        if (!state.running) {
            if (state.result != null) onClose() else confirmLeave = true
        }
    }
    BackHandler { leave() }

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = Space.sm, vertical = Space.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { leave() }) { Icon(Icons.Rounded.Close, contentDescription = "Close") }
                    Column {
                        Text("Stamp " + sessions.size + " photos", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Each photo is saved as a new copy. Originals stay untouched.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = Space.lg, vertical = Space.md),
                    verticalArrangement = Arrangement.spacedBy(Space.sm)
                ) {
                    if (state.running) {
                        LinearProgressIndicator(
                            progress = { if (state.count == 0) 0f else state.progress / state.count.toFloat() },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Stamping " + minOf(state.progress + 1, state.count) + " of " + state.count + "...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (state.errors.any) {
                        Text(
                            "Fix the fields marked in red first.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    PrimaryAction(
                        text = if (state.running) "Stamping..." else "Stamp " + state.count + " photos",
                        onClick = viewModel::run,
                        enabled = !state.running && !state.loading && !state.errors.any,
                        icon = Icons.Rounded.Save,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.lg, vertical = Space.md),
            verticalArrangement = Arrangement.spacedBy(Space.lg)
        ) {
            PreviewCard(state)
            PlaceCard(state, viewModel)
            DateCard(state, viewModel)
            StyleCard(state, viewModel)
            Spacer(Modifier.height(Space.lg))
        }
    }

    val result = state.result
    if (result != null) {
        AlertDialog(
            onDismissRequest = onClose,
            title = { Text(if (result.failed == 0) "All photos stamped" else "Finished with problems") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    Text("Saved " + result.saved + " new copies in Pictures/GeoStamp/Edited.")
                    if (result.withoutMap > 0) {
                        Text(result.withoutMap.toString() + " card(s) have no map picture because it could not be downloaded.")
                    }
                    if (result.failed > 0) {
                        Text(result.failed.toString() + " photo(s) could not be saved:", color = MaterialTheme.colorScheme.error)
                        result.problems.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = onClose) { Text("Done") } }
        )
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Leave without stamping?") },
            text = { Text("Nothing has been saved. Photos you just imported are removed from the app again.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLeave = false
                    viewModel.discardUnsaved()
                    onClose()
                }) { Text("Leave") }
            },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Keep editing") } }
        )
    }
}

@Composable
private fun PreviewCard(state: BatchState) {
    val preview = state.preview
    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp), contentAlignment = Alignment.Center) {
                if (preview != null) {
                    Image(
                        bitmap = preview.asImageBitmap(),
                        contentDescription = "Preview of the first photo with the stamp",
                        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large),
                        contentScale = ContentScale.FillWidth
                    )
                } else {
                    CircularProgressIndicator()
                }
            }
        }
        Text(
            "Preview of the first photo. Every photo keeps its own date and time unless you set one date below.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (state.mapNote != null) {
            Text(state.mapNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PlaceCard(state: BatchState, vm: BatchViewModel) {
    SectionCard(title = "Place and note", icon = Icons.Rounded.Place) {
        LabeledField("Place name, for example a school", state.fields.placeName, vm::setPlaceName)
        if (state.suggestions.isEmpty()) {
            Text(
                "Type a name or a group code such as a project name and pick a saved place. Its coordinates are filled in " +
                    "and applied to every photo. Leave the place empty to keep each photo's own location.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            PlaceSuggestionList(places = state.suggestions, onPick = vm::applyPlace, onHide = vm::dismissSuggestions)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            LabeledField(
                "Latitude", state.fields.latitude, vm::setLatitude,
                modifier = Modifier.weight(1f), error = state.errors.latitude, mono = true
            )
            LabeledField(
                "Longitude", state.fields.longitude, vm::setLongitude,
                modifier = Modifier.weight(1f), error = state.errors.longitude, mono = true
            )
        }
        LabeledField("Address (optional)", state.fields.address, vm::setAddress, singleLine = false)
        LabeledField("Note (optional), for example Visit 3", state.fields.label, vm::setLabel)
        if (state.canSavePlace) {
            TextButton(onClick = vm::savePlaceFromCurrent) { Text("Save this place for next time") }
        }
        if (state.message != null) {
            Text(state.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun DateCard(state: BatchState, vm: BatchViewModel) {
    SectionCard(title = "Date and time", icon = Icons.Rounded.Schedule) {
        ToggleRow(
            "Use one date and time for all photos",
            state.fields.setDateTime,
            vm::setSetDateTime,
            hint = "Off: every photo keeps its own date and time"
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            LabeledField(
                "Date", state.fields.date, vm::setDate,
                modifier = Modifier.weight(1f), error = state.errors.date, enabled = state.fields.setDateTime, mono = true
            )
            LabeledField(
                "Time", state.fields.time, vm::setTime,
                modifier = Modifier.weight(1f), error = state.errors.time, enabled = state.fields.setDateTime, mono = true
            )
        }
        LabeledField(
            "Time zone", state.fields.zone, vm::setZone,
            error = state.errors.zone, enabled = state.fields.setDateTime, mono = true
        )
    }
}

@Composable
private fun StyleCard(state: BatchState, vm: BatchViewModel) {
    val config = state.config
    SectionCard(title = "Stamp style", icon = Icons.Rounded.Tune) {
        Text(
            "Other style settings (colours, formats) use what you used last.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ChipRow {
            StampLayoutStyle.entries.forEach { style ->
                FilterChip(
                    selected = config.layoutStyle == style,
                    onClick = { vm.updateConfig { it.copy(layoutStyle = style) } },
                    label = { Text(if (style == StampLayoutStyle.BAR) "Classic bar" else "Map card") }
                )
            }
        }
        if (config.layoutStyle == StampLayoutStyle.CARD) {
            ToggleRow(
                "Show a small map picture",
                config.showMap,
                { on -> vm.updateConfig { it.copy(showMap = on) } },
                hint = "Downloads a small OpenStreetMap picture. This tells OpenStreetMap roughly where the place is."
            )
            if (state.mapFailed) TextButton(onClick = vm::retryMap) { Text("Try the map again") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Text size", style = MaterialTheme.typography.bodyMedium)
            Text(
                String.format(Locale.US, "%.1f%%", config.textSizeFraction * 100f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = config.textSizeFraction,
            onValueChange = { v -> vm.updateConfig { it.copy(textSizeFraction = v) } },
            valueRange = 0.020f..0.060f,
            modifier = Modifier.semantics { contentDescription = "Text size" }
        )
    }
}
