package com.samkit.duet.feature.camera

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samkit.duet.core.designsystem.components.CircleIconButton
import com.samkit.duet.core.designsystem.components.InfoPill
import com.samkit.duet.core.designsystem.icons.DuetIcons
import com.samkit.duet.core.model.CaptureLayout
import com.samkit.duet.core.model.FLASH
import com.samkit.duet.core.model.SplitDirection
import com.samkit.duet.core.ui.RecordingPill
import duet.shared.generated.resources.Res
import duet.shared.generated.resources.aspect_ratio_4_3
import duet.shared.generated.resources.change_layout
import duet.shared.generated.resources.flash_auto
import duet.shared.generated.resources.flash_off
import duet.shared.generated.resources.flash_on
import duet.shared.generated.resources.quality_default
import duet.shared.generated.resources.saving_two_files
import duet.shared.generated.resources.settings
import duet.shared.generated.resources.split_left_right
import duet.shared.generated.resources.split_top_bottom
import duet.shared.generated.resources.timer
import duet.shared.generated.resources.timer_off
import org.jetbrains.compose.resources.stringResource

/**
 * Flash · [center] · layout + settings.
 * [center] is a slot: each mode passes its own content, so the bar itself has no per-mode branches.
 */
@Composable
internal fun CameraTopBar(
    flash: FLASH,
    showLayoutButton: Boolean,
    onAction: (CameraAction) -> Unit,
    modifier: Modifier = Modifier,
    center: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(64.dp).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(
            onClick = { onAction(CameraAction.CycleFlash) },
            icon =
                when (flash) {
                    FLASH.AUTO -> DuetIcons.FlashAuto
                    FLASH.ON -> DuetIcons.Flash
                    FLASH.OFF -> DuetIcons.FlashOff
                },
            contentDescription =
                stringResource(
                    when (flash) {
                        FLASH.AUTO -> Res.string.flash_auto
                        FLASH.ON -> Res.string.flash_on
                        FLASH.OFF -> Res.string.flash_off
                    },
                ),
        )
        center()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (showLayoutButton) {
                CircleIconButton(
                    onClick = { onAction(CameraAction.OpenLayoutPicker) },
                    icon = DuetIcons.Layout,
                    contentDescription = stringResource(Res.string.change_layout),
                )
            }
            CircleIconButton(
                onClick = { onAction(CameraAction.OpenSettings) },
                icon = DuetIcons.Settings,
                contentDescription = stringResource(Res.string.settings),
            )
        }
    }
}

/** What goes in the top bar's center slot for each layout. */
@Composable
internal fun TopBarCenter(
    state: CameraUiState,
    onAction: (CameraAction) -> Unit,
) {
    when (state.layout) {
        CaptureLayout.SINGLE ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedPill(text = stringResource(Res.string.aspect_ratio_4_3))
                OutlinedPill(
                    text = stringResource(Res.string.timer_off),
                    leading = { Icon(DuetIcons.Timer, contentDescription = stringResource(Res.string.timer), Modifier.size(16.dp)) },
                )
            }
        CaptureLayout.PIP ->
            if (state.isRecording) RecordingPill(elapsed = state.elapsed) else InfoPill(text = stringResource(Res.string.quality_default))
        CaptureLayout.SPLIT -> SplitDirectionButton(state.splitDirection) { onAction(CameraAction.ToggleSplitDirection) }
        CaptureLayout.SEPARATE ->
            InfoPill(text = stringResource(Res.string.saving_two_files), leading = {
                Icon(DuetIcons.File, contentDescription = null, Modifier.size(16.dp))
            })
    }
}

@Composable
private fun OutlinedPill(
    text: String,
    leading: (@Composable () -> Unit)? = null,
) {
    InfoPill(
        text = text,
        leading = leading,
        height = 40.dp,
        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        textStyle = MaterialTheme.typography.labelLarge,
    )
}

@Composable
private fun SplitDirectionButton(
    direction: SplitDirection,
    onClick: () -> Unit,
) {
    val vertical = direction == SplitDirection.TopBottom
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.height(40.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (vertical) DuetIcons.SplitHorizontal else DuetIcons.SplitVertical,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(if (vertical) Res.string.split_top_bottom else Res.string.split_left_right),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
