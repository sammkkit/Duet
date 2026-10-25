package com.samkit.duet.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samkit.duet.core.designsystem.theme.DuetTheme
import com.samkit.duet.core.model.LENS

/** Row of zoom presets on a preview. The selected level shows "×" and uses the lens accent color. */
@Composable
fun ZoomChips(
    levels: List<Float>,
    selected: Float,
    onSelect: (Float) -> Unit,
    lens: LENS,
    modifier: Modifier = Modifier,
) {
    val accent = lens.colors().accent
    Row(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        levels.forEach { level ->
            val isSelected = level == selected
            val text = formatZoom(level)
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .selectable(selected = isSelected, onClick = { onSelect(level) }, role = Role.RadioButton)
                        .semantics { contentDescription = "Zoom ${text}x" },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isSelected) "$text×" else text,
                    color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface,
                    fontSize = if (isSelected) 14.sp else 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

// 0.5 -> ".5", 2.0 -> "2"
internal fun formatZoom(level: Float): String = if (level % 1f == 0f) level.toInt().toString() else level.toString().removePrefix("0")

@Preview
@Composable
private fun ZoomChipsPreview() {
    DuetTheme {
        Column(
            modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ZoomChips(levels = listOf(0.5f, 1f, 2f, 5f), selected = 1f, onSelect = {}, lens = LENS.BACK)
            ZoomChips(levels = listOf(1f), selected = 1f, onSelect = {}, lens = LENS.FRONT)
        }
    }
}
