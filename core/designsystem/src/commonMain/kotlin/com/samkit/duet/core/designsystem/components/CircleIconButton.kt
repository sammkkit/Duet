package com.samkit.duet.core.designsystem.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Round icon-only button used across the camera screens (flash, layout, settings, flip, swap).
 *
 * Layout and position come from [modifier]; [size] picks one of the design's sizes.
 */
@Composable
fun CircleIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = CircleIconButtonDefaults.Size,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.size(size),
        enabled = enabled,
        shape = CircleShape,
        colors =
            IconButtonDefaults.filledIconButtonColors(
                containerColor = containerColor,
                contentColor = contentColor,
            ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(CircleIconButtonDefaults.IconSize),
        )
    }
}

object CircleIconButtonDefaults {
    /** Top bar: flash, layout, settings. */
    val Size = 48.dp

    /** Bottom bar: flip, pause. */
    val LargeSize = 52.dp

    /** Overlaid on the preview: swap. */
    val SmallSize = 44.dp

    val IconSize = 22.dp
}
