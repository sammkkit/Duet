package com.samkit.duet.feature.camera

import com.samkit.duet.core.camera.CameraController
import com.samkit.duet.core.camera.CameraState
import com.samkit.duet.core.model.FLASH
import com.samkit.duet.core.model.SavedFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Duration

class FakeCameraController(
    var supportsDual: Boolean = true,
) : CameraController {
    override val state = MutableStateFlow<CameraState>(CameraState.Ready)

    var currentZoom = 1f
    var currentFlash = FLASH.OFF
    var photosTaken = 0
    var photoResult: Result<List<SavedFile>> = Result.success(listOf(SavedFile("fake-photo")))
    var videoResult: Result<SavedFile> = Result.success(SavedFile("fake-video"))

    override suspend fun supportsDualCamera() = supportsDual

    override suspend fun takePhoto(): Result<List<SavedFile>> {
        photosTaken++
        return photoResult
    }

    override fun startRecording() {
        state.value = CameraState.Recording(Duration.ZERO)
    }

    override suspend fun stopRecording(): Result<SavedFile> {
        state.value = CameraState.Ready
        return videoResult
    }

    override fun setZoom(ratio: Float) {
        currentZoom = ratio
    }

    override fun setFlash(mode: FLASH) {
        currentFlash = mode
    }
}
