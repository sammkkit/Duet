package com.samkit.duet.core.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.samkit.duet.core.model.LENS

@Composable
expect fun CameraPreview(
    lens: LENS,
    modifier: Modifier = Modifier,
)
