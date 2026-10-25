package com.samkit.duet.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samkit.duet.core.designsystem.components.SegmentedToggle
import com.samkit.duet.core.designsystem.theme.DuetTheme
import com.samkit.duet.core.model.CaptureType

/** Photo / Video. */
@Composable
fun CaptureTypeToggle(
    selected: CaptureType,
    onSelect: (CaptureType) -> Unit,
    modifier: Modifier = Modifier,
) {
    SegmentedToggle(
        items = CaptureType.entries,
        selected = selected,
        onSelect = onSelect,
        label = {
            when (it) {
                CaptureType.PHOTO -> "Photo"
                CaptureType.VIDEO -> "Video"
            }
        },
        modifier = modifier,
    )
}

@Preview
@Composable
private fun CaptureTypeTogglePreview() {
    DuetTheme {
        CaptureTypeToggle(
            selected = CaptureType.VIDEO,
            onSelect = {},
            modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp),
        )
    }
}
