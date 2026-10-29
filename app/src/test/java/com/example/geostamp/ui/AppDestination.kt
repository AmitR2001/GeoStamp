// geostamp-iteration: 7
package com.example.geostamp.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/** The three top-level tabs. */
enum class AppDestination(val label: String, val icon: ImageVector) {
    CAMERA("Camera", Icons.Rounded.CameraAlt),
    GALLERY("Gallery", Icons.Rounded.PhotoLibrary),
    SETTINGS("Settings", Icons.Rounded.Settings)
}
