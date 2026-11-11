package com.samkit.duet.core.permissions

import androidx.compose.runtime.Composable

enum class Permission { CAMERA, MICROPHONE }

sealed interface PermissionState {
    data object Granted : PermissionState

    data object Denied : PermissionState

    data object PermanentlyDenied : PermissionState
}

interface PermissionHandle {
    val state: PermissionState

    fun request()

    fun openSettings()
}

@Composable
expect fun rememberPermissionState(permission: Permission): PermissionHandle
