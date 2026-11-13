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

    /**
     * Two cameras can't stream photos and video at once, so dual layouts bind one or the other.
     * Called whenever the shutter switches between photo and video; ignored by a single camera, which binds both.
     */
    fun setVideoMode(enabled: Boolean)

    /**
     * Records every camera on screen (sound from the main camera when the mic is allowed); progress arrives as
     * [CameraState.Recording]. With [composition] and two cameras the stop merges them into one video that looks like
     * the screen (plus each camera's own video if [keepOriginals]); without it each camera saves its own video.
     */
    fun startRecording(
        composition: PhotoComposition? = null,
        keepOriginals: Boolean = false,
    )

    /** Stops and saves. Also returns the files when the camera already ended it (app backgrounded, interrupted). */
    suspend fun stopRecording(): Result<List<SavedFile>>

    fun setZoom(ratio: Float)

    fun setFlash(mode: FLASH)
}
