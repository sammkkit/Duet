package com.samkit.duet.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

internal val DuetShapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp), // split inner corners, file-name label
        medium = RoundedCornerShape(16.dp), // last-capture thumbnail
        large = RoundedCornerShape(20.dp), // PiP window
        extraLarge = RoundedCornerShape(28.dp), // preview area, dialog, sheet
    )
