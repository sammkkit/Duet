package com.samkit.duet.feature.camera

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.samkit.duet.core.designsystem.icons.DuetIcons
import com.samkit.duet.core.designsystem.theme.DuetTheme
import duet.shared.generated.resources.Res
import duet.shared.generated.resources.back_then_front
import duet.shared.generated.resources.dual_unavailable_body
import duet.shared.generated.resources.dual_unavailable_title
import duet.shared.generated.resources.use_single
import org.jetbrains.compose.resources.stringResource

/** Shown when a dual layout is picked on a phone that can't run both cameras together. */
@Composable
internal fun DualUnavailableDialog(onAction: (CameraAction) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAction(CameraAction.DismissDualUnavailable) },
        icon = { Icon(DuetIcons.CameraOff, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(stringResource(Res.string.dual_unavailable_title)) },
        text = { Text(stringResource(Res.string.dual_unavailable_body)) },
        confirmButton = {
            TextButton(onClick = { onAction(CameraAction.UseSingleCamera) }) { Text(stringResource(Res.string.use_single)) }
        },
        dismissButton = {
            TextButton(onClick = { onAction(CameraAction.CaptureBackThenFront) }) { Text(stringResource(Res.string.back_then_front)) }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}

@Preview
@Composable
private fun DualUnavailableDialogPreview() {
    DuetTheme { DualUnavailableDialog(onAction = {}) }
}
