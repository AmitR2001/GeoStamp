// geostamp-iteration: 7
package com.example.geostamp.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.geostamp.domain.places.SavedPlace
import com.example.geostamp.ui.design.Banner
import com.example.geostamp.ui.design.LabeledField
import com.example.geostamp.ui.design.PillTone
import com.example.geostamp.ui.design.PrimaryAction
import com.example.geostamp.ui.design.SecondaryAction
import com.example.geostamp.ui.design.SectionCard
import com.example.geostamp.ui.design.Space
import com.example.geostamp.ui.design.StatusPill
import kotlinx.coroutines.launch
import java.util.Locale

private fun granted(context: Context, permission: String) =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private fun versionName(context: Context): String =
    try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
    } catch (e: Exception) {
        "unknown"
    }

@Composable
fun SettingsScreen(modifier: Modifier = Modifier, viewModel: SettingsViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showAddPlace by remember { mutableStateOf(false) }
    var placeToDelete by remember { mutableStateOf<SavedPlace?>(null) }
    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.lg, vertical = Space.lg),
        verticalArrangement = Arrangement.spacedBy(Space.lg)
    ) {
        Column {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Everything stays on this phone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val message = state.message
        if (message != null) {
            Banner(text = message, tone = PillTone.Positive, icon = Icons.Rounded.Info)
        }

        SectionCard(title = "Saved places", icon = Icons.Rounded.Place) {
            Text(
                "Places you stamp often, such as schools. In the editor, type a name or a group code like a project name " +
                    "and pick one to fill in its name and coordinates.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            state.places.forEach { place ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(place.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            String.format(Locale.US, "%.6f, %.6f", place.latitude, place.longitude),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (place.tags.isNotEmpty()) {
                            Text(
                                place.tags.joinToString(", "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = { placeToDelete = place }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete " + place.name)
                    }
                }
            }
            SecondaryAction(text = "Add a place", onClick = { showAddPlace = true }, icon = Icons.Rounded.Place)
        }

        SectionCard(title = "Stamp style", icon = Icons.Rounded.Tune) {
            Text(
                "Your last used style is remembered for the next photo. Change it while editing a photo. " +
                    "This sample uses made-up data, not your location.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val sample = state.sample
            if (sample != null) {
                Image(
                    bitmap = sample.asImageBitmap(),
                    contentDescription = "Sample picture showing how your saved stamp style looks",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )
            }
            SecondaryAction(text = "Reset style to defaults", onClick = viewModel::resetStyle)
        }

        SectionCard(title = "Permissions", icon = Icons.Rounded.LocationOn) {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                if (granted(context, Manifest.permission.CAMERA)) StatusPill("Camera allowed", PillTone.Positive)
                else StatusPill("Camera not allowed", PillTone.Caution)
                when {
                    granted(context, Manifest.permission.ACCESS_FINE_LOCATION) -> StatusPill("Precise location", PillTone.Positive)
                    granted(context, Manifest.permission.ACCESS_COARSE_LOCATION) -> StatusPill("Approximate location", PillTone.Neutral)
                    else -> StatusPill("Location off", PillTone.Caution)
                }
            }
            Text(
                "Saving photos and picking photos need no permission.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SecondaryAction(text = "Open app settings", onClick = {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            })
        }

        SectionCard(title = "Storage", icon = Icons.Rounded.Folder) {
            Text(
                "GeoStamp keeps the original of every photo you saved so its stamp can be edited again. " +
                    "Leftover files from photos you took or imported but never saved: " +
                    state.leftoverCount + " (" + state.leftoverSize + ").",
                style = MaterialTheme.typography.bodyMedium
            )
            PrimaryAction(
                text = if (state.cleaning) "Cleaning..." else "Clean up leftover files",
                onClick = viewModel::cleanUp,
                enabled = !state.cleaning && state.leftoverCount > 0,
                modifier = Modifier.fillMaxWidth()
            )
        }

        SectionCard(title = "Privacy", icon = Icons.Rounded.Info) {
            Text(
                "GeoStamp has no account, no ads and no server. Photos and locations stay on this phone.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "The internet is used only when you ask for something that needs it: reading a Google Maps " +
                    "short link, looking up an address with Android's address service, or downloading the small " +
                    "OpenStreetMap picture for the map-card stamp. None of these sends a photo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("Version " + versionName(context), style = MaterialTheme.typography.labelSmall)
        }
    }

    if (showAddPlace) {
        AddPlaceDialog(viewModel = viewModel, onDismiss = { showAddPlace = false })
    }

    val deleting = placeToDelete
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { placeToDelete = null },
            title = { Text("Delete this place?") },
            text = { Text("\"" + deleting.name + "\" is removed from your saved places. Photos already stamped are not changed.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePlace(deleting)
                    placeToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { placeToDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AddPlaceDialog(viewModel: SettingsViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a place") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                LabeledField("Name, for example a school", name, { name = it })
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    LabeledField("Latitude", latitude, { latitude = it }, modifier = Modifier.weight(1f), mono = true)
                    LabeledField("Longitude", longitude, { longitude = it }, modifier = Modifier.weight(1f), mono = true)
                }
                LabeledField("Group codes, separated by commas (for example a project name)", tags, { tags = it })
                LabeledField("Or paste a Google Maps link here", link, { link = it })
                SecondaryAction(
                    text = "Read the link",
                    onClick = {
                        viewModel.lookupLink(link) { lat, lon, foundName ->
                            latitude = lat
                            longitude = lon
                            if (name.isBlank() && foundName != null) name = foundName
                        }
                    },
                    enabled = link.isNotBlank()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    if (viewModel.savePlace(name, latitude, longitude, tags)) onDismiss()
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
