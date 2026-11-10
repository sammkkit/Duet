package com.samkit.duet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.samkit.duet.core.designsystem.icons.DuetIcons

// List rows used by more than one screen in this module (sheet, Settings, Review).

/** Blue section title above a group of rows. */
@Composable
internal fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp),
    )
}

/** Title + description + switch; the whole row toggles. */
@Composable
internal fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onToggle: () -> Unit,
    horizontalPadding: Dp = 24.dp,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .toggleable(value = checked, role = Role.Switch, onValueChange = { onToggle() })
                .padding(horizontal = horizontalPadding, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TitleAndDescription(title, description, Modifier.weight(1f))
        // null: the row handles the click, so TalkBack reads one control instead of two.
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** Title + current value + chevron. Phase 7 makes these open pickers. */
@Composable
internal fun ValueRow(
    title: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TitleAndDescription(title, value, Modifier.weight(1f))
        Icon(
            DuetIcons.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
internal fun TitleAndDescription(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
