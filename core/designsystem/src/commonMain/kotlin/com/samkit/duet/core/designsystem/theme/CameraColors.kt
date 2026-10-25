package com.samkit.duet.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Colors for one camera: border/text accent, chip background, chip text. */
@Immutable
data class LensColors(
    val accent: Color,
    val container: Color,
    val onContainer: Color,
)

/** Camera colors that have no Material 3 role. Read them via DuetTheme.cameraColors. */
@Immutable
data class CameraColors(
    val back: LensColors,
    val front: LensColors,
    val focus: Color,
)

internal val DarkCameraColors =
    CameraColors(
        back = LensColors(accent = Blue80, container = Blue30, onContainer = Blue90),
        front = LensColors(accent = Orange80, container = Orange30, onContainer = Orange90),
        focus = Yellow80,
    )

internal val LocalCameraColors =
    staticCompositionLocalOf<CameraColors> { error("No CameraColors provided. Wrap your UI in DuetTheme { }.") }
