package com.samkit.duet.core.camera

import com.samkit.duet.core.model.FLASH
import com.samkit.duet.core.model.SavedFile
import kotlinx.coroutines.flow.StateFlow

interface CameraController {
    val state: StateFlow<CameraState>

    suspend fun supportsDualCamera(): Boolean

    /**
     * Captures every camera on screen. With [composition] and two cameras: one merged photo
     * (plus each camera's own photo if [keepOriginals]). Without it: one photo per camera.
     */
    suspend fun takePhoto(
        composition: PhotoComposition? = null,
        keepOriginals: Boolean = false,
    ): Result<List<SavedFile>>

    /** Single camera only. Records with sound when the mic permission is granted; progress arrives as [CameraState.Recording]. */
    fun startRecording()

    /** Stops and saves the video. Also returns the file when the camera already ended it (app backgrounded, interrupted). */
    suspend fun stopRecording(): Result<SavedFile>

    fun setZoom(ratio: Float)

    fun setFlash(mode: FLASH)
}
