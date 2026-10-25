package com.samkit.duet.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samkit.duet.core.designsystem.components.InfoPill
import com.samkit.duet.core.designsystem.theme.DuetTheme
import com.samkit.duet.core.model.LENS

/** "BACK" / "FRONT" label on a preview, in that camera's colors. [small] is the one inside the PiP window. */
@Composable
fun CameraChip(
    lens: LENS,
    modifier: Modifier = Modifier,
    label: String = lens.name,
    small: Boolean = false,
) {
    val colors = lens.colors()
    InfoPill(
        text = label,
        modifier = modifier,
        height = if (small) 22.dp else 28.dp,
        containerColor = colors.container,
        contentColor = colors.onContainer,
        textStyle = if (small) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
        contentPadding = PaddingValues(horizontal = if (small) 8.dp else 12.dp),
    )
}

@Preview
@Composable
private fun CameraChipPreview() {
    DuetTheme {
        Row(
            modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CameraChip(lens = LENS.BACK, label = "BACK · MAIN")
            CameraChip(lens = LENS.BACK)
            CameraChip(lens = LENS.FRONT)
            CameraChip(lens = LENS.FRONT, small = true)
        }
    }
}
