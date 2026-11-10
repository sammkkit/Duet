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
import duet.core.ui.generated.resources.Res
import duet.core.ui.generated.resources.layout_pip
import duet.core.ui.generated.resources.layout_separate
import duet.core.ui.generated.resources.layout_single
import duet.core.ui.generated.resources.layout_split
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Single · Video call · Split · Separate. */
@Composable
fun ModeTabs(
    selected: CaptureLayout,
    onSelect: (CaptureLayout) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Resolve labels here: PillTabs' label lambda isn't @Composable, so it can't call stringResource.
    val labels = CaptureLayout.entries.associateWith { stringResource(it.label()) }
    PillTabs(
        items = CaptureLayout.entries,
        selected = selected,
        onSelect = onSelect,
        label = { labels.getValue(it) },
        modifier = modifier,
    )
}

// No else branch: a new CaptureLayout won't compile until it gets a label.
private fun CaptureLayout.label(): StringResource =
    when (this) {
        CaptureLayout.SINGLE -> Res.string.layout_single
        CaptureLayout.PIP -> Res.string.layout_pip
        CaptureLayout.SPLIT -> Res.string.layout_split
        CaptureLayout.SEPARATE -> Res.string.layout_separate
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
