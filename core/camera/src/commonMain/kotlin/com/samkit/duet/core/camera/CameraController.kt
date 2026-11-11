package com.samkit.duet.core.camera

import com.samkit.duet.core.model.FLASH
import com.samkit.duet.core.model.LENS
import com.samkit.duet.core.model.SavedFile
import kotlinx.coroutines.flow.StateFlow

interface CameraController {
    val state: StateFlow<CameraState>

    suspend fun bind(lens: LENS)

    suspend fun takePhoto(): Result<SavedFile>

    fun startRecording()

    suspend fun stopRecording(): Result<SavedFile>

    fun setZoom(ratio: Float)

    fun setFlash(mode: FLASH)
}
