// geostamp-iteration: 8
package com.example.geostamp.ui.design

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.geostamp.domain.places.SavedPlace
import java.util.Locale

/** A list of matching saved places shown under a text field. Tapping one picks it. */
@Composable
fun PlaceSuggestionList(
    places: List<SavedPlace>,
    onPick: (SavedPlace) -> Unit,
    onHide: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            places.forEach { place ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClickLabel = "Use " + place.name) { onPick(place) }
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
            TextButton(onClick = onHide) { Text("Hide suggestions") }
        }
    }
}
