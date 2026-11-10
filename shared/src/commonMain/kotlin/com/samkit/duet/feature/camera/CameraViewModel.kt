package com.samkit.duet.feature.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samkit.duet.core.model.CaptureLayout
import com.samkit.duet.core.model.CaptureType
import com.samkit.duet.core.model.FLASH
import com.samkit.duet.core.model.LENS
import com.samkit.duet.core.model.PipCorner
import com.samkit.duet.core.model.SplitDirection
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class CameraViewModel : ViewModel() {
    private val _state = MutableStateFlow(CameraUiState())
    val state: StateFlow<CameraUiState> = _state.asStateFlow()

    private var timerJob: Job? = null

    fun onLayoutSelected(layout: CaptureLayout) = updateUnlessLocked { it.copy(layout = layout) }

    fun onCaptureTypeSelected(type: CaptureType) = updateUnlessLocked { it.copy(captureType = type) }

    fun onSeparateTypeSelected(
        lens: LENS,
        type: CaptureType,
    ) = updateUnlessLocked {
        when (lens) {
            LENS.BACK -> it.copy(backType = type)
            LENS.FRONT -> it.copy(frontType = type)
        }
    }

    fun onShutterClick() {
        val current = _state.value
        when {
            current.isRecording -> stopRecording()
            recordsVideo(current) -> startRecording()
            // Photo: nothing to do until Phase 2 adds the camera.
        }
    }

    fun onSwapCameras() =
        _state.update {
            it.copy(mainLens = if (it.mainLens == LENS.BACK) LENS.FRONT else LENS.BACK)
        }

    fun onZoomSelected(level: Float) = _state.update { it.copy(zoom = level) }

    fun onFlashClick() =
        _state.update {
            val next =
                when (it.flash) {
                    FLASH.AUTO -> FLASH.ON
                    FLASH.ON -> FLASH.OFF
                    FLASH.OFF -> FLASH.AUTO
                }
            it.copy(flash = next)
        }

    fun onSplitDirectionToggle() =
        _state.update {
            val next = if (it.splitDirection == SplitDirection.TopBottom) SplitDirection.LeftRight else SplitDirection.TopBottom
            it.copy(splitDirection = next)
        }

    fun onSplitRatioChange(ratio: Float) = _state.update { it.copy(splitRatio = ratio.coerceIn(MIN_SPLIT_RATIO, MAX_SPLIT_RATIO)) }

    fun onPipCornerChange(corner: PipCorner) = _state.update { it.copy(pipCorner = corner) }

    // ponytail: fake timer that ticks +1 s; Phase 2 replaces it with CameraX's recorded duration.
    private fun startRecording() {
        _state.update { it.copy(isRecording = true, elapsed = Duration.ZERO) }
        timerJob?.cancel()
        timerJob =
            viewModelScope.launch {
                while (isActive) {
                    delay(1.seconds)
                    _state.update { it.copy(elapsed = it.elapsed + 1.seconds) }
                }
            }
    }

    private fun stopRecording() {
        timerJob?.cancel()
        timerJob = null
        _state.update { it.copy(isRecording = false) }
    }

    // Separate mode records if either camera is set to video.
    private fun recordsVideo(state: CameraUiState): Boolean =
        if (state.layout == CaptureLayout.SEPARATE) {
            state.backType == CaptureType.VIDEO || state.frontType == CaptureType.VIDEO
        } else {
            state.captureType == CaptureType.VIDEO
        }

    private inline fun updateUnlessLocked(crossinline transform: (CameraUiState) -> CameraUiState) =
        _state.update { if (it.controlsLocked) it else transform(it) }
}
