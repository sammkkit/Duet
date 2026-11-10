package com.samkit.duet.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

// Dark only: a camera viewfinder stays dark regardless of system theme.
private val DuetColorScheme =
    darkColorScheme(
        primary = Blue80,
        onPrimary = Blue20,
        primaryContainer = Blue30,
        onPrimaryContainer = Blue90,
        secondary = BlueGrey80,
        onSecondary = BlueGrey20,
        secondaryContainer = BlueGrey30,
        onSecondaryContainer = BlueGrey90,
        background = Neutral4,
        onBackground = Neutral90,
        surface = Neutral4,
        onSurface = Neutral90,
        surfaceVariant = NeutralVariant30,
        onSurfaceVariant = NeutralVariant80,
        surfaceContainerLowest = Neutral6,
        surfaceContainerLow = Neutral10,
        surfaceContainer = Neutral12,
        surfaceContainerHigh = Neutral17,
        surfaceContainerHighest = Neutral22,
        outline = NeutralVariant60,
        outlineVariant = NeutralVariant30,
        error = Red60,
        onError = Red20,
        errorContainer = Red10,
        onErrorContainer = Red90,
    )

@Composable
fun DuetTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalCameraColors provides DarkCameraColors) {
        MaterialTheme(
            colorScheme = DuetColorScheme,
            typography = DuetTypography,
            shapes = DuetShapes,
            content = content,
        )
    }
}

object DuetTheme {
    val cameraColors: CameraColors
        @Composable @ReadOnlyComposable
        get() = LocalCameraColors.current
}
