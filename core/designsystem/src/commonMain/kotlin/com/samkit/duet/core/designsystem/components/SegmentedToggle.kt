package com.samkit.duet.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.samkit.duet.core.designsystem.theme.DuetTheme

/**
 * Pick one of a few options (Photo/Video, Back/Front, Rounded/Circle).
 * Thin wrapper over M3's segmented button, which already matches the design
 * (40 dp, outline border, secondaryContainer + check mark when selected).
 */
@Composable
fun <T> SegmentedToggle(
    items: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    segmentWidth: Dp = SegmentedToggleDefaults.SegmentWidth,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        items.forEachIndexed { index, item ->
            SegmentedButton(
                selected = item == selected,
                onClick = { onSelect(item) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = items.size),
                modifier = Modifier.width(segmentWidth),
                label = { Text(label(item)) },
            )
        }
    }
}

object SegmentedToggleDefaults {
    /** Photo/Video toggle on the camera screen. */
    val SegmentWidth = 104.dp

    /** Back/Front and Rounded/Circle in the layout sheet. */
    val CompactSegmentWidth = 88.dp
}

@Preview
@Composable
private fun SegmentedTogglePreview() {
    DuetTheme {
        Column(
            modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SegmentedToggle(items = listOf("Photo", "Video"), selected = "Video", onSelect = {}, label = { it })
            SegmentedToggle(
                items = listOf("Back", "Front"),
                selected = "Back",
                onSelect = {},
                label = { it },
                segmentWidth = SegmentedToggleDefaults.CompactSegmentWidth,
            )
        }
    }
}
