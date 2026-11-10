package com.samkit.duet.feature.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samkit.duet.core.model.CaptureLayout
import com.samkit.duet.core.model.CaptureType
import com.samkit.duet.core.model.FLASH
import com.samkit.duet.core.model.LENS
import com.samkit.duet.core.model.SplitDirection
import com.samkit.duet.core.settings.SettingsRepository
import com.samkit.duet.core.settings.UserSettings
import duet.shared.generated.resources.Res
import duet.shared.generated.resources.msg_back_then_front
import duet.shared.generated.resources.msg_photo_taken
import duet.shared.generated.resources.msg_photos_both_taken
import duet.shared.generated.resources.msg_stop_recording_first
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class CameraViewModel(
    private val settingsRepository: SettingsRepository,
    // Phase 4 passes the real hardware check; flip to false to try the dialog.
    private val dualCameraSupported: Boolean = true,
) : ViewModel() {
    private val _state = MutableStateFlow(CameraUiState())
    val state: StateFlow<CameraUiState> = _state.asStateFlow()

    /** Shared with the Settings screen, so it's a separate stream rather than a copy inside [state]. */
    val settings: StateFlow<UserSettings> = settingsRepository.settings

    // Channel, not StateFlow: each effect is delivered once and not replayed after rotation.
    private val _effects = Channel<CameraEffect>(Channel.BUFFERED)
    val effects: Flow<CameraEffect> = _effects.receiveAsFlow()

    private var timerJob: Job? = null

    /** The single entry point for user input. */
    fun onAction(action: CameraAction) {
        when (action) {
            is CameraAction.SelectLayout -> selectLayout(action.layout)
            is CameraAction.SelectCaptureType -> updateUnlessLocked { it.copy(captureType = action.type) }
            is CameraAction.SelectSeparateType ->
                updateUnlessLocked {
                    when (action.lens) {
                        LENS.BACK -> it.copy(backType = action.type)
                        LENS.FRONT -> it.copy(frontType = action.type)
                    }
                }
            CameraAction.ShutterClick -> onShutterClick()
            CameraAction.SnapBothPhotos -> emit(CameraEffect.ShowMessage(Res.string.msg_photos_both_taken))
            CameraAction.SwapCameras -> _state.update { it.copy(mainLens = it.mainLens.other()) }
            is CameraAction.SelectMainLens -> _state.update { it.copy(mainLens = action.lens) }
            is CameraAction.SelectZoom -> _state.update { it.copy(zoom = action.level) }
            CameraAction.CycleFlash -> _state.update { it.copy(flash = it.flash.next()) }
            CameraAction.ToggleSplitDirection -> _state.update { it.copy(splitDirection = it.splitDirection.other()) }
            is CameraAction.ChangeSplitRatio ->
                _state.update { it.copy(splitRatio = action.ratio.coerceIn(MIN_SPLIT_RATIO, MAX_SPLIT_RATIO)) }
            is CameraAction.ChangePipCorner -> _state.update { it.copy(pipCorner = action.corner) }
            is CameraAction.SelectPipShape -> _state.update { it.copy(pipShape = action.shape) }
            CameraAction.ToggleKeepOriginals -> settingsRepository.update { it.copy(keepOriginals = !it.keepOriginals) }
            CameraAction.OpenLayoutPicker -> updateUnlessLocked { it.copy(isLayoutPickerOpen = true) }
            CameraAction.DismissLayoutPicker -> _state.update { it.copy(isLayoutPickerOpen = false) }
            CameraAction.UseSingleCamera -> _state.update { it.copy(layout = CaptureLayout.SINGLE, isDualUnavailableOpen = false) }
            CameraAction.CaptureBackThenFront -> {
                _state.update { it.copy(isDualUnavailableOpen = false) }
                emit(CameraEffect.ShowMessage(Res.string.msg_back_then_front))
            }
            CameraAction.DismissDualUnavailable -> _state.update { it.copy(isDualUnavailableOpen = false) }
            CameraAction.OpenSettings -> emit(CameraEffect.NavigateToSettings)
            CameraAction.OpenLastCapture -> emit(CameraEffect.NavigateToReview)
        }
    }

    private fun selectLayout(layout: CaptureLayout) {
        if (layout != CaptureLayout.SINGLE && !dualCameraSupported) {
            _state.update { it.copy(isDualUnavailableOpen = true, isLayoutPickerOpen = false) }
        } else {
            updateUnlessLocked { it.copy(layout = layout) }
        }
    }

    private fun onShutterClick() {
        val current = _state.value
        when {
            current.isRecording -> stopRecording()
            current.shutterType == CaptureType.VIDEO -> startRecording()
            else -> emit(CameraEffect.ShowMessage(Res.string.msg_photo_taken))
        }
    }

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

    private fun updateUnlessLocked(transform: (CameraUiState) -> CameraUiState) {
        if (_state.value.controlsLocked) {
            emit(CameraEffect.ShowMessage(Res.string.msg_stop_recording_first))
        } else {
            _state.update(transform)
        }
    }

    private fun emit(effect: CameraEffect) {
        _effects.trySend(effect)
    }
}

private fun LENS.other() = if (this == LENS.BACK) LENS.FRONT else LENS.BACK

private fun SplitDirection.other() = if (this == SplitDirection.TopBottom) SplitDirection.LeftRight else SplitDirection.TopBottom

private fun FLASH.next() =
    when (this) {
        FLASH.AUTO -> FLASH.ON
        FLASH.ON -> FLASH.OFF
        FLASH.OFF -> FLASH.AUTO
    }
