package com.samkit.duet.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samkit.duet.core.designsystem.theme.DuetTheme

/**
 * A centered row of text tabs; the selected one sits in a filled pill.
 * Generic over [T] so camera-aware wrappers (ModeTabs) can pass their own enum.
 */
@Composable
fun <T> PillTabs(
    items: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
    ) {
        items.forEach { item ->
            PillTab(
                text = label(item),
                selected = item == selected,
                onClick = { onSelect(item) },
            )
        }
    }
}

@Composable
private fun PillTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(if (selected) colors.primaryContainer else Color.Transparent)
    val content by animateColorAsState(if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant)

    Box(
        modifier =
            Modifier
                .height(44.dp)
                .clip(CircleShape)
                .background(container)
                .selectable(selected = selected, onClick = onClick, role = Role.Tab)
                .padding(horizontal = if (selected) 16.dp else 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = content)
    }
}

@Preview
@Composable
private fun PillTabsPreview() {
    DuetTheme {
        Box(Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            PillTabs(
                items = listOf("Single", "Video call", "Split", "Separate"),
                selected = "Video call",
                onSelect = {},
                label = { it },
            )
        }
    }
}
