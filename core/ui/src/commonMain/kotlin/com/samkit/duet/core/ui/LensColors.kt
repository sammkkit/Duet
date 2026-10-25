package com.samkit.duet.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.samkit.duet.core.designsystem.theme.DuetTheme
import com.samkit.duet.core.designsystem.theme.LensColors
import com.samkit.duet.core.model.LENS

/** The one place a lens is mapped to its colors (designsystem can't know LENS, model can't know colors). */
@Composable
@ReadOnlyComposable
fun LENS.colors(): LensColors =
    when (this) {
        LENS.BACK -> DuetTheme.cameraColors.back
        LENS.FRONT -> DuetTheme.cameraColors.front
    }
