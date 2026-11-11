package com.samkit.duet.feature.camera

import com.samkit.duet.core.camera.CameraController
import com.samkit.duet.core.camera.CameraState
import com.samkit.duet.core.model.FLASH
import com.samkit.duet.core.model.LENS
import com.samkit.duet.core.model.SavedFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Duration

class FakeCameraController : CameraController {
    override val state = MutableStateFlow<CameraState>(CameraState.Idle)

    var boundLens: LENS? = null
    var currentZoom = 1f
    var currentFlash = FLASH.OFF
    var photosTaken = 0
    var nextResult: Result<SavedFile> = Result.success(SavedFile("fake-id"))

    override suspend fun bind(lens: LENS) {
        boundLens = lens
        state.value = CameraState.Ready
    }

    override suspend fun takePhoto(): Result<SavedFile> {
        photosTaken++
        return nextResult
    }

    override fun startRecording() {
        state.value = CameraState.Recording(Duration.ZERO)
    }

    override suspend fun stopRecording(): Result<SavedFile> {
        state.value = CameraState.Ready
        return nextResult
    }

    override fun setZoom(ratio: Float) {
        currentZoom = ratio
    }

    override fun setFlash(mode: FLASH) {
        currentFlash = mode
    }
}
