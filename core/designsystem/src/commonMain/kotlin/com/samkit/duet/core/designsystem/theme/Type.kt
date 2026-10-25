package com.samkit.duet.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ponytail: FontFamily.Default is Roboto on Android but SF on iOS.
// Bundle Roboto in composeResources/font when iOS is turned on.
private val Roboto = FontFamily.Default

internal val DuetTypography =
    Typography(
        // Dialog title ("Dual camera isn't available")
        headlineSmall = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Normal, fontSize = 24.sp, lineHeight = 32.sp),
        // Screen titles (Settings, Review)
        titleLarge = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 28.sp),
        // Settings rows, layout card titles
        titleMedium =
            TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
        // Dialog body, settings descriptions
        bodyMedium =
            TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
        // Hints, quality pill ("1080p · 30 fps")
        bodySmall = TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
        // Tabs, buttons, Photo/Video toggle
        labelLarge =
            TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
        // BACK / FRONT chips
        labelMedium =
            TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
        // Small FRONT chip in the PiP window
        labelSmall =
            TextStyle(fontFamily = Roboto, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    )
