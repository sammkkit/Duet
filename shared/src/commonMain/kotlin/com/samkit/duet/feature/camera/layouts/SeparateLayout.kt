package com.samkit.duet.feature.camera.layouts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.samkit.duet.core.designsystem.components.InfoPill
import com.samkit.duet.core.designsystem.components.InfoPillDefaults
import com.samkit.duet.core.designsystem.icons.DuetIcons
import com.samkit.duet.core.model.CaptureType
import com.samkit.duet.core.model.LENS
import com.samkit.duet.core.ui.CameraChip
import com.samkit.duet.core.ui.RecordingPill
import com.samkit.duet.core.ui.colors
import com.samkit.duet.feature.camera.CameraAction
import com.samkit.duet.feature.camera.CameraUiState
import duet.shared.generated.resources.Res
import duet.shared.generated.resources.back_photo
import duet.shared.generated.resources.back_video
import duet.shared.generated.resources.front_photo
import duet.shared.generated.resources.front_video
import org.jetbrains.compose.resources.stringResource

/** Both cameras at once, each with its own photo/video choice and its own file. */
@Composable
internal fun SeparateLayout(
    state: CameraUiState,
    onAction: (CameraAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CameraPanel(LENS.BACK, state.backType, state, onAction, Modifier.weight(1f).fillMaxWidth())
        CameraPanel(LENS.FRONT, state.frontType, state, onAction, Modifier.weight(1f).fillMaxWidth())
    }
}

@Composable
private fun CameraPanel(
    lens: LENS,
    type: CaptureType,
    state: CameraUiState,
    onAction: (CameraAction) -> Unit,
    modifier: Modifier,
) {
    PreviewSurface(lens = lens, modifier = modifier, shape = RoundedCornerShape(24.dp), bordered = true) {
        Row(
            modifier = Modifier.align(Alignment.TopStart).fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                CameraChip(lens = lens)
                if (state.isRecording && type == CaptureType.VIDEO) RecordingPill(elapsed = state.elapsed)
            }
            LensTypeToggle(lens = lens, selected = type, onSelect = { onAction(CameraAction.SelectSeparateType(lens, it)) })
        }
        InfoPill(
            text = fakeFileName(lens, type),
            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
            height = InfoPillDefaults.SmallHeight,
            shape = MaterialTheme.shapes.small,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            textStyle = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            contentPadding = InfoPillDefaults.SmallContentPadding,
        )
    }
}

/** Photo/video icon pair for one camera. Feature-local: only this screen uses it. */
@Composable
private fun LensTypeToggle(
    lens: LENS,
    selected: CaptureType,
    onSelect: (CaptureType) -> Unit,
) {
    val lensColors = lens.colors()
    Row(
        Modifier
            .height(44.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, CircleShape)
            .padding(2.dp)
            .selectableGroup(),
    ) {
        CaptureType.entries.forEach { type ->
            val isSelected = type == selected
            Box(
                modifier =
                    Modifier
                        .width(48.dp)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(if (isSelected) lensColors.container else Color.Transparent)
                        .selectable(selected = isSelected, onClick = { onSelect(type) }, role = Role.RadioButton),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (type == CaptureType.PHOTO) DuetIcons.Photo else DuetIcons.Video,
                    contentDescription = stringResource(lensTypeDescription(lens, type)),
                    tint = if (isSelected) lensColors.onContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

private fun lensTypeDescription(
    lens: LENS,
    type: CaptureType,
) = when (lens) {
    LENS.BACK -> if (type == CaptureType.PHOTO) Res.string.back_photo else Res.string.back_video
    LENS.FRONT -> if (type == CaptureType.PHOTO) Res.string.front_photo else Res.string.front_video
}

// ponytail: fixed example names; core:media's FileNaming produces real ones in Phase 2.
private fun fakeFileName(
    lens: LENS,
    type: CaptureType,
): String {
    val suffix = lens.name.lowercase()
    return if (type == CaptureType.VIDEO) "VID_20261025_1842_$suffix.mp4" else "IMG_20261025_1842_${suffix}_012.jpg"
}
