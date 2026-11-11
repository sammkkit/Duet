package com.samkit.duet.feature.camera

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.samkit.duet.core.camera.rememberThumbnail
import com.samkit.duet.core.designsystem.components.CircleIconButton
import com.samkit.duet.core.designsystem.components.CircleIconButtonDefaults
import com.samkit.duet.core.designsystem.icons.DuetIcons
import com.samkit.duet.core.model.CaptureLayout
import com.samkit.duet.core.model.LENS
import com.samkit.duet.core.model.SavedFile
import com.samkit.duet.core.ui.CaptureTypeToggle
import com.samkit.duet.core.ui.ModeTabs
import com.samkit.duet.core.ui.ShutterButton
import com.samkit.duet.core.ui.colors
import duet.shared.generated.resources.Res
import duet.shared.generated.resources.open_last_capture
import duet.shared.generated.resources.pause_recording
import duet.shared.generated.resources.separate_hint
import duet.shared.generated.resources.snap_both
import duet.shared.generated.resources.switch_to_back
import duet.shared.generated.resources.switch_to_front
import org.jetbrains.compose.resources.stringResource

/** Photo/Video toggle (or Separate's hint) · mode tabs · thumbnail / shutter / secondary button. */
@Composable
internal fun CameraBottomBar(
    state: CameraUiState,
    onAction: (CameraAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.layout == CaptureLayout.SEPARATE) {
            Text(
                text = stringResource(Res.string.separate_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.height(40.dp).wrapContentHeight(),
            )
        } else {
            CaptureTypeToggle(selected = state.captureType, onSelect = { onAction(CameraAction.SelectCaptureType(it)) })
        }

        ModeTabs(selected = state.layout, onSelect = { onAction(CameraAction.SelectLayout(it)) })

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LastCaptureThumbnail(state.lastCapture, onClick = { onAction(CameraAction.OpenLastCapture) })
            ShutterButton(
                captureType = state.shutterType,
                isRecording = state.isRecording,
                onClick = { onAction(CameraAction.ShutterClick) },
            )
            SecondaryButton(state, onAction)
        }
    }
}

/** Right of the shutter: flip, pause or "snap both", depending on the layout. */
@Composable
private fun SecondaryButton(
    state: CameraUiState,
    onAction: (CameraAction) -> Unit,
) {
    when {
        state.layout == CaptureLayout.SEPARATE ->
            CircleIconButton(
                onClick = { onAction(CameraAction.SnapBothPhotos) },
                icon = DuetIcons.Photo,
                contentDescription = stringResource(Res.string.snap_both),
                size = CircleIconButtonDefaults.LargeSize,
            )
        state.layout == CaptureLayout.PIP && state.isRecording ->
            // Pause needs a real recorder (Phase 2); shown disabled so the layout matches the design.
            CircleIconButton(
                onClick = {},
                icon = DuetIcons.Pause,
                contentDescription = stringResource(Res.string.pause_recording),
                size = CircleIconButtonDefaults.LargeSize,
                enabled = false,
            )
        else ->
            CircleIconButton(
                onClick = { onAction(CameraAction.SwapCameras) },
                icon = DuetIcons.Flip,
                contentDescription =
                    stringResource(
                        if (state.mainLens ==
                            LENS.BACK
                        ) {
                            Res.string.switch_to_front
                        } else {
                            Res.string.switch_to_back
                        },
                    ),
                size = CircleIconButtonDefaults.LargeSize,
            )
    }
}

@Composable
private fun LastCaptureThumbnail(
    file: SavedFile?,
    onClick: () -> Unit,
) {
    val shape = MaterialTheme.shapes.medium
    val description = stringResource(Res.string.open_last_capture)
    Box(
        Modifier
            .size(52.dp)
            .clip(shape)
            .background(LENS.BACK.colors().placeholder)
            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        val thumbnail = file?.let { rememberThumbnail(it) }
        if (thumbnail != null) {
            Image(thumbnail, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
        }
    }
}
