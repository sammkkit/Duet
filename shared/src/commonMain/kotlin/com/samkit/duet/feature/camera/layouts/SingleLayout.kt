package com.samkit.duet.feature.camera.layouts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.samkit.duet.core.designsystem.theme.DuetTheme
import com.samkit.duet.core.ui.CameraChip
import com.samkit.duet.core.ui.ZoomChips
import com.samkit.duet.feature.camera.CameraAction
import com.samkit.duet.feature.camera.CameraUiState

private val SingleZoomLevels = listOf(0.5f, 1f, 2f, 5f)

/** One camera, rule-of-thirds grid, focus square, zoom row. */
@Composable
internal fun SingleLayout(
    state: CameraUiState,
    onAction: (CameraAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    PreviewSurface(lens = state.mainLens, modifier = modifier) {
        ThirdsGrid(Modifier.matchParentSize())
        Box(
            Modifier
                .align(BiasAlignment(0f, -0.08f))
                .size(72.dp)
                .border(2.dp, DuetTheme.cameraColors.focus, RoundedCornerShape(12.dp)),
        )
        CameraChip(lens = state.mainLens, modifier = Modifier.align(Alignment.TopStart).padding(16.dp))
        ZoomChips(
            levels = SingleZoomLevels,
            selected = state.zoom,
            onSelect = { onAction(CameraAction.SelectZoom(it)) },
            lens = state.mainLens,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
        )
    }
}

@Composable
private fun ThirdsGrid(modifier: Modifier) {
    Canvas(modifier) {
        val line = Color.White.copy(alpha = 0.28f)
        val stroke = 1.dp.toPx()
        for (i in 1..2) {
            val x = size.width * i / 3
            val y = size.height * i / 3
            drawLine(line, Offset(x, 0f), Offset(x, size.height), stroke)
            drawLine(line, Offset(0f, y), Offset(size.width, y), stroke)
        }
    }
}
