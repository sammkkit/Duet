package com.samkit.duet.feature.camera.layouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samkit.duet.core.model.LENS
import com.samkit.duet.core.ui.CameraChip
import com.samkit.duet.feature.camera.CameraAction
import com.samkit.duet.feature.camera.CameraUiState

/** Both cameras at once; the shutter saves one photo per camera. */
@Composable
internal fun SeparateLayout(
    state: CameraUiState,
    onAction: (CameraAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CameraPanel(LENS.BACK, Modifier.weight(1f).fillMaxWidth())
        CameraPanel(LENS.FRONT, Modifier.weight(1f).fillMaxWidth())
    }
}

@Composable
private fun CameraPanel(
    lens: LENS,
    modifier: Modifier,
) {
    PreviewSurface(lens = lens, modifier = modifier, shape = RoundedCornerShape(24.dp), bordered = true) {
        CameraChip(lens = lens, modifier = Modifier.align(Alignment.TopStart).padding(12.dp))
    }
}
