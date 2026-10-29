// geostamp-iteration: 7
package com.example.geostamp.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.RotateLeft
import androidx.compose.material.icons.rounded.RotateRight
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.geostamp.data.media.SaveMode
import com.example.geostamp.domain.model.EditorSession
import com.example.geostamp.domain.stamp.DateStyle
import com.example.geostamp.domain.stamp.LocationSource
import com.example.geostamp.domain.stamp.StampAlignment
import com.example.geostamp.domain.stamp.StampEditing
import com.example.geostamp.domain.stamp.StampLayoutStyle
import com.example.geostamp.domain.stamp.TimeStyle
import com.example.geostamp.ui.common.openMaps
import com.example.geostamp.ui.common.readClipboardText
import com.example.geostamp.ui.common.shareImage
import com.example.geostamp.ui.design.ChipRow
import com.example.geostamp.ui.design.LabeledField
import com.example.geostamp.ui.design.PillTone
import com.example.geostamp.ui.design.PrimaryAction
import com.example.geostamp.ui.design.SecondaryAction
import com.example.geostamp.ui.design.SectionCard
import com.example.geostamp.ui.design.Space
import com.example.geostamp.ui.design.StatusPill
import com.example.geostamp.ui.design.ToggleRow
import java.util.Locale

@Composable
fun EditorScreen(
    session: EditorSession,
    sharedText: String?,
    onSharedConsumed: () -> Unit,
    onClose: () -> Unit,
    viewModel: EditorViewModel = viewModel(key = session.id)
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmReplace by remember { mutableStateOf(false) }

    LaunchedEffect(session.id) { viewModel.start(session) }

    val saved = state.result != null
    val leave: () -> Unit = { if (saved) onClose() else confirmDiscard = true }
    BackHandler { leave() }

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { EditorTopBar(state, onClose = { leave() }) },
        bottomBar = {
            SaveBar(
                state = state,
                onSave = { viewModel.save(SaveMode.NEW_COPY) },
                onReplace = { confirmReplace = true }
            )
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
            PreviewCard(state, viewModel)
            PlaceCard(state, viewModel)
            MapCard(state, viewModel, sharedText, onSharedConsumed)
            WhenCard(state, viewModel)
            StyleCard(state, viewModel)
            Spacer(Modifier.height(Space.lg))
        }
    }

    val result = state.result
    if (result != null) {
        AlertDialog(
            onDismissRequest = onClose,
            title = { Text("Saved") },
            text = {
                Text(
                    (if (result.replaced) "Replaced the saved photo:\n" else "Saved a new copy to\n") + result.path + "\n\n" +
                        (if (result.exifCopied) "The original camera metadata was copied to the saved photo."
                        else "The original camera metadata could not be copied. The picture itself is saved.") +
                        (if (result.mapMissing) "\n\nThe map picture could not be downloaded, so the card was saved without it." else "")
                )
            },
            confirmButton = { TextButton(onClick = onClose) { Text("Done") } },
            dismissButton = { TextButton(onClick = { shareImage(context, result.uri) }) { Text("Share") } }
        )
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(if (state.isExisting) "Close without saving?" else "Discard this photo?") },
            text = {
                Text(
                    if (state.isExisting) "Your changes are lost. The saved photo is not changed."
                    else "It has not been saved. If you discard it, it is deleted from the app."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    viewModel.discard()
                    onClose()
                }) { Text(if (state.isExisting) "Close" else "Discard") }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } }
        )
    }

    if (confirmReplace) {
        AlertDialog(
            onDismissRequest = { confirmReplace = false },
            title = { Text("Replace the saved photo?") },
            text = {
                Text(
                    "The file in your gallery will be overwritten with this stamped version. " +
                        "This cannot be undone. Saving a copy instead keeps the old one."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmReplace = false
                    viewModel.save(SaveMode.REPLACE)
                }) { Text("Replace") }
            },
            dismissButton = { TextButton(onClick = { confirmReplace = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun EditorTopBar(state: EditorState, onClose: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = Space.sm, vertical = Space.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Close the editor") }
            Column(modifier = Modifier.weight(1f)) {
                Text("Edit stamp", style = MaterialTheme.typography.titleLarge)
                Text(
                    if (state.isExisting) "Saved photo" else "New photo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            when (state.source) {
                LocationSource.GPS -> StatusPill("GPS location", PillTone.Positive, icon = Icons.Rounded.Place)
                LocationSource.MANUAL -> StatusPill("Typed by you", PillTone.Caution, icon = Icons.Rounded.Edit)
                LocationSource.UNAVAILABLE -> StatusPill("No location", PillTone.Neutral)
            }
            Spacer(Modifier.padding(end = Space.sm))
        }
    }
}

@Composable
private fun SaveBar(state: EditorState, onSave: () -> Unit, onReplace: () -> Unit) {
    val canSave = !state.saving && !state.loading && state.loadError == null && !state.errors.hasErrors
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Space.lg, vertical = Space.md),
            verticalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            if (state.saveError != null) {
                Text(state.saveError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (state.errors.hasErrors) {
                Text(
                    "Fix the fields marked in red to save.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalAlignment = Alignment.CenterVertically) {
                PrimaryAction(
                    text = if (state.saving) "Saving..." else "Save copy to " + state.saveFolder,
                    onClick = onSave,
                    enabled = canSave,
                    icon = Icons.Rounded.Save,
                    modifier = Modifier.weight(1f)
                )
                if (state.canReplace) {
                    SecondaryAction(text = "Replace", onClick = onReplace, enabled = canSave)
                }
            }
        }
    }
}

@Composable
private fun PreviewCard(state: EditorState, vm: EditorViewModel) {
    val preview = state.preview
    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    state.loadError != null -> Text(
                        state.loadError,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(Space.lg)
                    )
                    preview != null -> Image(
                        bitmap = preview.asImageBitmap(),
                        contentDescription = "Preview of the photo with the stamp at the bottom",
                        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large),
                        contentScale = ContentScale.FillWidth
                    )
                    else -> CircularProgressIndicator()
                }
            }
        }
        if (state.mapNote != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    state.mapNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (state.mapFailed) TextButton(onClick = vm::retryMap) { Text("Try again") }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Turned " + state.rotation + "\u00B0. Rotation is built into the saved copy; the original is never changed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { vm.rotate(-90) }, enabled = !state.loading) {
                Icon(Icons.Rounded.RotateLeft, contentDescription = "Rotate left")
            }
            IconButton(onClick = { vm.rotate(90) }, enabled = !state.loading) {
                Icon(Icons.Rounded.RotateRight, contentDescription = "Rotate right")
            }
        }
    }
}

@Composable
private fun PlaceCard(state: EditorState, vm: EditorViewModel) {
    SectionCard(title = "Place", icon = Icons.Rounded.Place) {
        PlaceNameField(state, vm)
        Text(state.gpsNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (state.gpsLookup == GpsLookup.SEARCHING) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        if (state.canUseGps) {
            SecondaryAction(text = "Use the GPS location", onClick = vm::useGpsFix, icon = Icons.Rounded.MyLocation)
        }
        LabeledField("Address", state.fields.address, vm::setAddress, singleLine = false)
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
        LabeledField("Label (optional)", state.fields.label, vm::setLabel)
    }
}

@Composable
private fun PlaceNameField(state: EditorState, vm: EditorViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        LabeledField("Place name, for example a school", state.fields.placeName, vm::setPlaceName)
        if (state.placeSuggestions.isEmpty()) {
            Text(
                "Type a name or a group code such as a project name to pick one of your saved places. " +
                    "Its coordinates are filled in for you. The name is written before the address on the stamp.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column {
                    state.placeSuggestions.forEach { place ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClickLabel = "Use " + place.name) { vm.applyPlace(place) }
                                .padding(horizontal = Space.lg, vertical = Space.md)
                        ) {
                            Text(place.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                String.format(Locale.US, "%.6f, %.6f", place.latitude, place.longitude) +
                                    (if (place.tags.isNotEmpty()) "   " + place.tags.joinToString(", ") else ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    TextButton(onClick = vm::dismissPlaceSuggestions) { Text("Hide suggestions") }
                }
            }
        }
        if (state.canSavePlace) {
            TextButton(onClick = vm::savePlaceFromCurrent) { Text("Save this place for next time") }
        }
        if (state.placeMessage != null) {
            Text(state.placeMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun MapCard(state: EditorState, vm: EditorViewModel, sharedText: String?, onSharedConsumed: () -> Unit) {
    val context = LocalContext.current
    var pasted by remember { mutableStateOf("") }
    SectionCard(title = "Pick the place in Google Maps", icon = Icons.Rounded.Map) {
        Text(
            "Open Maps and find the place. Then tap Share and choose GeoStamp, or copy the link " +
                "(or the coordinates shown when you long-press a spot) and paste it below.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (sharedText != null) {
            Text(
                "Shared from Maps: " + sharedText.lineSequence().firstOrNull().orEmpty().take(80),
                style = MaterialTheme.typography.bodyMedium
            )
            PrimaryAction(
                text = "Use the shared location",
                onClick = {
                    vm.useMapText(sharedText)
                    onSharedConsumed()
                },
                enabled = !state.mapBusy,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            SecondaryAction(
                text = "Open Maps",
                icon = Icons.Rounded.Map,
                onClick = {
                    val lat = StampEditing.parseLatitude(state.fields.latitude)
                    val lon = StampEditing.parseLongitude(state.fields.longitude)
                    if (!openMaps(context, lat, lon)) vm.showMapMessage("No maps app was found on this phone.")
                }
            )
            SecondaryAction(
                text = "Paste",
                icon = Icons.Rounded.ContentPaste,
                onClick = {
                    val text = readClipboardText(context)
                    if (text != null) pasted = text else vm.showMapMessage("The clipboard is empty.")
                }
            )
        }
        LabeledField("Maps link or coordinates", pasted, { pasted = it })
        PrimaryAction(
            text = if (state.mapBusy) "Reading..." else "Use this location",
            onClick = { vm.useMapText(pasted) },
            enabled = pasted.isNotBlank() && !state.mapBusy,
            modifier = Modifier.fillMaxWidth()
        )
        if (state.mapMessage != null) {
            Text(state.mapMessage, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            "Coordinates and long links are read on the phone. Short links (maps.app.goo.gl) need one quick " +
                "internet request to find the place; nothing of yours is sent.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WhenCard(state: EditorState, vm: EditorViewModel) {
    SectionCard(title = "Date and time", icon = Icons.Rounded.Schedule) {
        Text(state.capturedText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            LabeledField(
                "Date", state.fields.date, vm::setDate,
                modifier = Modifier.weight(1f), error = state.errors.date, mono = true
            )
            LabeledField(
                "Time", state.fields.time, vm::setTime,
                modifier = Modifier.weight(1f), error = state.errors.time, mono = true
            )
        }
        LabeledField("Time zone (for example Asia/Kolkata)", state.fields.zone, vm::setZone, error = state.errors.zone, mono = true)
        SecondaryAction(text = "Reset to when the photo was taken", onClick = vm::resetDateTime)
        Text(
            "These fields only change the text drawn on the picture. The photo's own camera metadata (EXIF) is copied unchanged.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StyleCard(state: EditorState, vm: EditorViewModel) {
    val config = state.config
    SectionCard(title = "Stamp style", icon = Icons.Rounded.Tune) {
        SliderRow(
            label = "Text size",
            valueText = String.format(Locale.US, "%.1f%%", config.textSizeFraction * 100f),
            value = config.textSizeFraction,
            range = 0.020f..0.060f
        ) { v -> vm.updateConfig { it.copy(textSizeFraction = v) } }
        SliderRow(
            label = "Padding",
            valueText = String.format(Locale.US, "%.1f%%", config.paddingFraction * 100f),
            value = config.paddingFraction,
            range = 0.010f..0.050f
        ) { v -> vm.updateConfig { it.copy(paddingFraction = v) } }

        ChipGroup("Stamp layout") {
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
        }
        ChipGroup("Alignment (classic bar)") {
            StampAlignment.entries.forEach { alignment ->
                FilterChip(
                    selected = config.alignment == alignment,
                    onClick = { vm.updateConfig { it.copy(alignment = alignment) } },
                    label = {
                        Text(
                            when (alignment) {
                                StampAlignment.START -> "Left"
                                StampAlignment.CENTER -> "Center"
                                StampAlignment.END -> "Right"
                            }
                        )
                    }
                )
            }
        }
        ChipGroup("Text colour") {
            ForegroundPresets.forEach { preset ->
                FilterChip(
                    selected = config.foregroundColor == preset.argb,
                    onClick = { vm.updateConfig { it.copy(foregroundColor = preset.argb) } },
                    label = { Text(preset.name) }
                )
            }
        }
        ChipGroup("Background") {
            BackgroundPresets.forEach { preset ->
                FilterChip(
                    selected = config.backgroundColor == preset.argb,
                    onClick = { vm.updateConfig { it.copy(backgroundColor = preset.argb) } },
                    label = { Text(preset.name) }
                )
            }
        }
        ChipGroup("Date format (classic bar)") {
            DateStyle.entries.forEach { style ->
                FilterChip(
                    selected = config.dateStyle == style,
                    onClick = { vm.updateConfig { it.copy(dateStyle = style) } },
                    label = {
                        Text(
                            when (style) {
                                DateStyle.DAY_MONTH_YEAR -> "13 Oct 2026"
                                DateStyle.MONTH_DAY_YEAR -> "Oct 13, 2026"
                                DateStyle.ISO -> "2026-10-13"
                            }
                        )
                    }
                )
            }
        }
        ChipGroup("Time format (classic bar)") {
            TimeStyle.entries.forEach { style ->
                FilterChip(
                    selected = config.timeStyle == style,
                    onClick = { vm.updateConfig { it.copy(timeStyle = style) } },
                    label = { Text(if (style == TimeStyle.H24) "24-hour" else "12-hour") }
                )
            }
        }
        ToggleRow("Show time zone", config.showTimeZone, { on -> vm.updateConfig { it.copy(showTimeZone = on) } })
        ToggleRow("Show label", config.showLabel, { on -> vm.updateConfig { it.copy(showLabel = on) } })
    }
}

@Composable
private fun SliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                valueText,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.semantics { contentDescription = label + " " + valueText }
        )
    }
}

@Composable
private fun ChipGroup(title: String, chips: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ChipRow { chips() }
    }
}
