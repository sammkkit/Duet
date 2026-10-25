package com.samkit.duet.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samkit.duet.core.designsystem.components.PillTabs
import com.samkit.duet.core.designsystem.theme.DuetTheme
import com.samkit.duet.core.model.CaptureLayout

/** Single · Video call · Split · Separate. */
@Composable
fun ModeTabs(
    selected: CaptureLayout,
    onSelect: (CaptureLayout) -> Unit,
    modifier: Modifier = Modifier,
) {
    PillTabs(
        items = CaptureLayout.entries,
        selected = selected,
        onSelect = onSelect,
        label = { it.label() },
        modifier = modifier,
    )
}

// No else branch: a new CaptureLayout won't compile until it gets a label.
private fun CaptureLayout.label(): String =
    when (this) {
        CaptureLayout.SINGLE -> "Single"
        CaptureLayout.PIP -> "Video call"
        CaptureLayout.SPLIT -> "Split"
        CaptureLayout.SEPARATE -> "Separate"
    }

@Preview
@Composable
private fun ModeTabsPreview() {
    DuetTheme {
        ModeTabs(
            selected = CaptureLayout.PIP,
            onSelect = {},
            modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp),
        )
    }
}
