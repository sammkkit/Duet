package com.samkit.duet.feature.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samkit.duet.core.designsystem.icons.DuetIcons
import com.samkit.duet.core.designsystem.theme.DuetTheme
import com.samkit.duet.core.permissions.Permission
import com.samkit.duet.core.permissions.PermissionState
import com.samkit.duet.core.permissions.rememberPermissionState
import duet.shared.generated.resources.Res
import duet.shared.generated.resources.permission_camera_blocked_body
import duet.shared.generated.resources.permission_camera_body
import duet.shared.generated.resources.permission_camera_title
import duet.shared.generated.resources.permission_grant
import duet.shared.generated.resources.permission_open_settings
import org.jetbrains.compose.resources.stringResource

/**
 * Shows [content] only once the camera permission is granted; otherwise explains why it's needed.
 * Asks automatically once on first open, then waits for the user to tap the button.
 */
@Composable
fun CameraPermissionGate(content: @Composable () -> Unit) {
    val camera = rememberPermissionState(Permission.CAMERA)

    // rememberSaveable: rotating after a denial must not pop the system dialog again.
    var askedOnce by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!askedOnce && camera.state == PermissionState.Denied) {
            askedOnce = true
            camera.request()
        }
    }

    when (camera.state) {
        PermissionState.Granted -> content()
        PermissionState.Denied -> CameraPermissionScreen(blocked = false, onAction = camera::request)
        PermissionState.PermanentlyDenied -> CameraPermissionScreen(blocked = true, onAction = camera::openSettings)
    }
}

/** [blocked]: the user chose "Don't ask again", so the button opens system Settings instead of the dialog. */
@Composable
internal fun CameraPermissionScreen(
    blocked: Boolean,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing).padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                DuetIcons.CameraOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )
            Text(
                stringResource(Res.string.permission_camera_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(if (blocked) Res.string.permission_camera_blocked_body else Res.string.permission_camera_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onAction,
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth().height(48.dp),
            ) {
                Text(stringResource(if (blocked) Res.string.permission_open_settings else Res.string.permission_grant))
            }
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun CameraPermissionScreenPreview() {
    DuetTheme { CameraPermissionScreen(blocked = false, onAction = {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun CameraPermissionBlockedPreview() {
    DuetTheme { CameraPermissionScreen(blocked = true, onAction = {}) }
}
