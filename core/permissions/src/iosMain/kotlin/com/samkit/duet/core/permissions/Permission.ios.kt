package com.samkit.duet.core.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeAudio
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun rememberPermissionState(permission: Permission): PermissionHandle {
    val mediaType = permission.toMediaType()
    val handle = remember(mediaType) { IosPermissionHandle(mediaType) }
    // Coming back from the Settings app: pick up a permission granted there.
    LifecycleResumeEffect(mediaType) {
        handle.refresh()
        onPauseOrDispose { }
    }
    return handle
}

private class IosPermissionHandle(
    private val mediaType: String?,
) : PermissionHandle {
    override var state: PermissionState by mutableStateOf(currentState())

    fun refresh() {
        state = currentState()
    }

    // iOS shows the system prompt only once; after a "Don't Allow" only Settings can change it.
    override fun request() {
        if (state == PermissionState.PermanentlyDenied) return
        AVCaptureDevice.requestAccessForMediaType(mediaType) { _ ->
            dispatch_async(dispatch_get_main_queue()) { refresh() }
        }
    }

    override fun openSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any>(), null)
    }

    private fun currentState(): PermissionState =
        when (AVCaptureDevice.authorizationStatusForMediaType(mediaType)) {
            AVAuthorizationStatusAuthorized -> PermissionState.Granted
            AVAuthorizationStatusNotDetermined -> PermissionState.Denied
            else -> PermissionState.PermanentlyDenied // denied or restricted (parental controls)
        }
}

private fun Permission.toMediaType(): String? =
    when (this) {
        Permission.CAMERA -> AVMediaTypeVideo
        Permission.MICROPHONE -> AVMediaTypeAudio
    }
