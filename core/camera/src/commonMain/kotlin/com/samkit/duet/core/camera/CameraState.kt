package com.samkit.duet.core.camera

import kotlin.time.Duration

sealed interface CameraState {
    data object Idle : CameraState

    data object Ready : CameraState

    data class Recording(
        val elapsed: Duration,
    ) : CameraState

    data class Error(
        val reason: CameraError,
    ) : CameraState
}

enum class CameraError { NO_CAMERA, CAMERA_IN_USE, UNKNOWN }
