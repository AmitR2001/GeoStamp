// geostamp-iteration: 7
package com.example.geostamp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = GeoForest,
    onPrimary = Color.White,
    primaryContainer = GeoForestContainer,
    onPrimaryContainer = GeoOnForestContainer,
    secondary = GeoAmber,
    onSecondary = Color.White,
    secondaryContainer = GeoAmberContainer,
    onSecondaryContainer = GeoOnAmberContainer,
    tertiary = GeoSlate,
    onTertiary = Color.White,
    tertiaryContainer = GeoSlateContainer,
    background = GeoPaper,
    onBackground = GeoInk,
    surface = GeoSurface,
    onSurface = GeoInk,
    surfaceVariant = GeoSurfaceVariant,
    onSurfaceVariant = GeoInkSoft,
    outline = GeoOutline,
    outlineVariant = GeoOutlineSoft
)

private val DarkColors = darkColorScheme(
    primary = GeoMint,
    onPrimary = Color(0xFF00382A),
    primaryContainer = GeoMintContainer,
    onPrimaryContainer = GeoOnMintContainer,
    secondary = GeoSand,
    onSecondary = Color(0xFF3F2800),
    secondaryContainer = GeoSandContainer,
    onSecondaryContainer = GeoOnSandContainer,
    tertiary = GeoFog,
    onTertiary = Color(0xFF0B2A3B),
    background = GeoNight,
    onBackground = GeoNightInk,
    surface = GeoNightSurface,
    onSurface = GeoNightInk,
    surfaceVariant = GeoNightSurfaceVariant,
    onSurfaceVariant = GeoNightInkSoft,
    outline = GeoNightOutline,
    outlineVariant = GeoNightOutlineSoft
)

private val GeoShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

/** The GeoStamp look: paper-and-forest colours, serif headings, soft rounded shapes, no dynamic colour. */
@Composable
fun GeoStampTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = GeoTypography,
        shapes = GeoShapes,
        content = content
    )
}
