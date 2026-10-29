// geostamp-iteration: 7
package com.example.geostamp.ui.design

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/** Spacing scale used everywhere, so the layout keeps one rhythm. */
object Space {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

enum class PillTone { Positive, Caution, Neutral, Danger }

/** A small rounded label such as "GPS location" or "Typed by you". */
@Composable
fun StatusPill(text: String, tone: PillTone, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val scheme = MaterialTheme.colorScheme
    val container: Color
    val content: Color
    when (tone) {
        PillTone.Positive -> { container = scheme.primaryContainer; content = scheme.onPrimaryContainer }
        PillTone.Caution -> { container = scheme.secondaryContainer; content = scheme.onSecondaryContainer }
        PillTone.Neutral -> { container = scheme.surfaceVariant; content = scheme.onSurfaceVariant }
        PillTone.Danger -> { container = scheme.errorContainer; content = scheme.onErrorContainer }
    }
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
        Text(text, color = content, style = MaterialTheme.typography.labelMedium)
    }
}

/** A titled card with a thin outline instead of a heavy shadow. */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(Space.lg), verticalArrangement = Arrangement.spacedBy(Space.md)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            content()
        }
    }
}

/** A highlighted message with an optional icon and action. */
@Composable
fun Banner(
    text: String,
    modifier: Modifier = Modifier,
    tone: PillTone = PillTone.Neutral,
    icon: ImageVector? = null,
    action: (@Composable () -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    val container: Color
    val content: Color
    when (tone) {
        PillTone.Positive -> { container = scheme.primaryContainer; content = scheme.onPrimaryContainer }
        PillTone.Caution -> { container = scheme.secondaryContainer; content = scheme.onSecondaryContainer }
        PillTone.Neutral -> { container = scheme.surfaceVariant; content = scheme.onSurfaceVariant }
        PillTone.Danger -> { container = scheme.errorContainer; content = scheme.onErrorContainer }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(container)
            .padding(Space.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.md)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
        Text(text, color = content, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        if (action != null) action()
    }
}

/** The brand mark: two map rings and a location pin, drawn in code (no image files). */
@Composable
fun GeoMark(modifier: Modifier = Modifier, markSize: Dp = 96.dp) {
    val ring = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    val pinColor = MaterialTheme.colorScheme.primary
    val dot = MaterialTheme.colorScheme.surface
    Canvas(modifier = modifier.size(markSize)) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val radius = this.size.minDimension / 2f
        val stroke = Stroke(width = radius * 0.05f)
        drawCircle(color = ring, radius = radius * 0.94f, center = center, style = stroke)
        drawCircle(color = ring, radius = radius * 0.66f, center = center, style = stroke)
        val headRadius = radius * 0.22f
        val headCenter = Offset(center.x, center.y - radius * 0.1f)
        val tip = Offset(center.x, center.y + radius * 0.42f)
        val path = Path().apply {
            moveTo(headCenter.x - headRadius, headCenter.y)
            lineTo(tip.x, tip.y)
            lineTo(headCenter.x + headRadius, headCenter.y)
            close()
        }
        drawPath(path = path, color = pinColor)
        drawCircle(color = pinColor, radius = headRadius, center = headCenter)
        drawCircle(color = dot, radius = headRadius * 0.4f, center = headCenter)
    }
}

/** A calm, friendly page for "nothing here yet". */
@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier.padding(Space.xl),
        verticalArrangement = Arrangement.spacedBy(Space.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GeoMark(markSize = 112.dp)
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (action != null) {
            Spacer(Modifier.size(Space.xs))
            action()
        }
    }
}

@Composable
fun PrimaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 52.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(Space.sm))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Space.sm))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** A text field with a quiet error message; [mono] shows numbers in a typewriter font. */
@Composable
fun LabeledField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    enabled: Boolean = true,
    mono: Boolean = false,
    singleLine: Boolean = true
) {
    val style = if (mono) LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace) else LocalTextStyle.current
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        textStyle = style,
        label = { Text(label) },
        isError = enabled && error != null,
        supportingText = if (enabled && error != null) {
            { Text(error) }
        } else {
            null
        },
        singleLine = singleLine,
        shape = MaterialTheme.shapes.small
    )
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier, hint: String? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { },
        horizontalArrangement = Arrangement.spacedBy(Space.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (hint != null) {
                Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** A horizontally scrolling row for chips. */
@Composable
fun ChipRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        verticalAlignment = Alignment.CenterVertically
    ) { content() }
}

/** Makes the status-bar icons light (for dark screens such as the camera) while this is on screen. */
@Composable
fun SystemBarIcons(lightIcons: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        DisposableEffect(lightIcons) {
            val window = (view.context as? Activity)?.window
            val controller = if (window != null) WindowCompat.getInsetsController(window, view) else null
            val previous = controller?.isAppearanceLightStatusBars
            controller?.isAppearanceLightStatusBars = !lightIcons
            onDispose {
                if (previous != null) controller?.isAppearanceLightStatusBars = previous
            }
        }
    }
}
