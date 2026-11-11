package com.samkit.duet.core.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

@Composable
actual fun rememberPermissionState(permission: Permission): PermissionHandle {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val androidPermission = permission.toAndroid()

    val handle = remember(androidPermission) { AndroidPermissionHandle(context.stateOf(androidPermission), context) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            handle.state =
                when {
                    granted -> PermissionState.Granted
                    activity.canAskAgain(androidPermission) -> PermissionState.Denied
                    else -> PermissionState.PermanentlyDenied
                }
        }
    handle.onRequest = { launcher.launch(androidPermission) }
    LifecycleResumeEffect(androidPermission) {
        if (context.stateOf(androidPermission) == PermissionState.Granted) handle.state = PermissionState.Granted
        onPauseOrDispose { }
    }
    return handle
}

private fun Activity?.canAskAgain(permission: String): Boolean = this?.shouldShowRequestPermissionRationale(permission) ?: true

private class AndroidPermissionHandle(
    initial: PermissionState,
    private val context: Context,
) : PermissionHandle {
    override var state: PermissionState by mutableStateOf(initial)

    var onRequest: () -> Unit = {}

    override fun request() = onRequest()

    override fun openSettings() {
        val intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

private fun Permission.toAndroid(): String =
    when (this) {
        Permission.CAMERA -> Manifest.permission.CAMERA
        Permission.MICROPHONE -> Manifest.permission.RECORD_AUDIO
    }

private fun Context.stateOf(permission: String): PermissionState =
    if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
        PermissionState.Granted
    } else {
        PermissionState.Denied
    }
