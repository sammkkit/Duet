package com.samkit.duet.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.samkit.duet.core.designsystem.theme.DuetTheme

/**
 * Small read-only label: "1080p · 30 fps", "Saving 2 files", file names.
 * [leading] is a slot so callers can put an icon or a dot (RecordingPill) before the text.
 */
@Composable
fun InfoPill(
    text: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    height: Dp = InfoPillDefaults.Height,
    shape: Shape = CircleShape,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    textStyle: TextStyle = MaterialTheme.typography.bodySmall,
    contentPadding: PaddingValues = InfoPillDefaults.ContentPadding,
) {
    Row(
        modifier =
            modifier
                .height(height)
                .background(containerColor, shape)
                .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            leading?.invoke()
            Text(text = text, style = textStyle, color = contentColor, maxLines = 1)
        }
    }
}

object InfoPillDefaults {
    /** Top bar pills. */
    val Height = 32.dp

    /** Pills inside the preview (BACK/FRONT chips, timers). */
    val SmallHeight = 28.dp

    val ContentPadding = PaddingValues(horizontal = 14.dp)
    val SmallContentPadding = PaddingValues(horizontal = 10.dp)
}

@Preview
@Composable
private fun InfoPillPreview() {
    DuetTheme {
        Column(
            modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InfoPill(text = "1080p · 30 fps")
            InfoPill(
                text = "00:42",
                leading = { Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.error, CircleShape)) },
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                textStyle = MaterialTheme.typography.labelLarge,
            )
            InfoPill(
                text = "VID_20261025_1842_back.mp4",
                height = InfoPillDefaults.SmallHeight,
                shape = MaterialTheme.shapes.small,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                contentPadding = InfoPillDefaults.SmallContentPadding,
            )
        }
    }
}
