package com.samkit.duet.feature.camera

import com.samkit.duet.core.camera.CameraController
import com.samkit.duet.core.camera.CameraState
import com.samkit.duet.core.camera.PhotoComposition
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
    var videoResult: Result<List<SavedFile>> = Result.success(listOf(SavedFile("fake-video")))
    var currentVideoMode = false
    var lastVideoComposition: PhotoComposition? = null

    override suspend fun supportsDualCamera() = supportsDual

    var lastComposition: PhotoComposition? = null
    var lastKeepOriginals = false

    override suspend fun takePhoto(
        composition: PhotoComposition?,
        keepOriginals: Boolean,
    ): Result<List<SavedFile>> {
        photosTaken++
        lastComposition = composition
        lastKeepOriginals = keepOriginals
        return photoResult
    }

    override fun setVideoMode(enabled: Boolean) {
        currentVideoMode = enabled
    }

    override fun startRecording(
        composition: PhotoComposition?,
        keepOriginals: Boolean,
    ) {
        lastVideoComposition = composition
        state.value = CameraState.Recording(Duration.ZERO)
    }

    override suspend fun stopRecording(): Result<List<SavedFile>> {
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
