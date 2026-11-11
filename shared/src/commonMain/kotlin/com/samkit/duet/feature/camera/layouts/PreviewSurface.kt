package com.samkit.duet.feature.camera.layouts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.samkit.duet.core.camera.CameraPreview
import com.samkit.duet.core.model.LENS
import com.samkit.duet.core.ui.colors

/** One camera's live preview, on its placeholder color until the first frame arrives. */
@Composable
internal fun PreviewSurface(
    lens: LENS,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    bordered: Boolean = false,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val colors = lens.colors()
    Box(
        modifier =
            modifier
                .clip(shape)
                .background(colors.placeholder)
                .then(if (bordered) Modifier.border(2.dp, colors.accent, shape) else Modifier),
    ) {
        CameraPreview(lens = lens, modifier = Modifier.matchParentSize())
        content()
    }
}

internal fun LENS.other(): LENS = if (this == LENS.BACK) LENS.FRONT else LENS.BACK
