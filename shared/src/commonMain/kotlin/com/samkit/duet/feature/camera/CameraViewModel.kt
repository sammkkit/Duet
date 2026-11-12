package com.samkit.duet.feature.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samkit.duet.core.camera.CameraController
import com.samkit.duet.core.camera.CameraState
import com.samkit.duet.core.camera.PhotoComposition
import com.samkit.duet.core.model.CaptureLayout
import com.samkit.duet.core.model.CaptureType
import com.samkit.duet.core.model.FLASH
import com.samkit.duet.core.model.LENS
import com.samkit.duet.core.model.SplitDirection
import com.samkit.duet.core.settings.SettingsRepository
import com.samkit.duet.core.settings.UserSettings
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration

class CameraViewModel(
    private val settingsRepository: SettingsRepository,
    private val camera: CameraController,
) : ViewModel() {
    // Assume yes until the hardware answers (a few ms after start), so the first tap isn't wrongly blocked.
    private var dualCameraSupported = true
    private val _state = MutableStateFlow(CameraUiState())
    val state: StateFlow<CameraUiState> = _state.asStateFlow()

    /** Shared with the Settings screen, so it's a separate stream rather than a copy inside [state]. */
    val settings: StateFlow<UserSettings> = settingsRepository.settings

    // Channel, not StateFlow: each effect is delivered once and not replayed after rotation.
    private val _effects = Channel<CameraEffect>(Channel.BUFFERED)
    val effects: Flow<CameraEffect> = _effects.receiveAsFlow()

    // Set by the camera's first Recording update: before that, "not recording" just means it hasn't started yet.
    private var recordingStarted = false

    init {
        viewModelScope.launch { dualCameraSupported = camera.supportsDualCamera() }
        // The camera binds photo or video streams in dual layouts, so it follows what the shutter will do.
        viewModelScope.launch {
            _state.map { it.shutterType == CaptureType.VIDEO }.distinctUntilChanged().collect { camera.setVideoMode(it) }
        }
        viewModelScope.launch {
            camera.state.collect { cameraState ->
                when {
                    cameraState is CameraState.Recording -> {
                        recordingStarted = true
                        _state.update { it.copy(elapsed = cameraState.elapsed) }
                    }
                    // The camera ended it on its own (app backgrounded, call came in): collect the file all the same.
                    recordingStarted && _state.value.isRecording -> stopRecording()
                }
            }
        }
    }

    /** The single entry point for user input. */
    fun onAction(action: CameraAction) {
        when (action) {
            is CameraAction.SelectLayout -> selectLayout(action.layout)
            is CameraAction.SelectCaptureType -> updateUnlessLocked { it.copy(captureType = action.type) }
            CameraAction.ShutterClick -> onShutterClick()
            // Locked while recording: switching cameras would end the recording.
            CameraAction.SwapCameras -> updateUnlessLocked { it.copy(mainLens = it.mainLens.other()) }
            is CameraAction.SelectMainLens -> _state.update { it.copy(mainLens = action.lens) }
            is CameraAction.SelectZoom -> {
                _state.update { it.copy(zoom = action.level) }
                camera.setZoom(action.level)
            }
            CameraAction.CycleFlash -> {
                _state.update { it.copy(flash = it.flash.next()) }
                camera.setFlash(_state.value.flash)
            }
            CameraAction.ToggleSplitDirection -> _state.update { it.copy(splitDirection = it.splitDirection.other()) }
            is CameraAction.ChangeSplitRatio ->
                _state.update { it.copy(splitRatio = action.ratio.coerceIn(MIN_SPLIT_RATIO, MAX_SPLIT_RATIO)) }
            is CameraAction.ChangePipCorner -> _state.update { it.copy(pipCorner = action.corner) }
            is CameraAction.SelectPipShape -> _state.update { it.copy(pipShape = action.shape) }
            CameraAction.ToggleKeepOriginals -> settingsRepository.update { it.copy(keepOriginals = !it.keepOriginals) }
            CameraAction.OpenLayoutPicker -> updateUnlessLocked { it.copy(isLayoutPickerOpen = true) }
            CameraAction.DismissLayoutPicker -> _state.update { it.copy(isLayoutPickerOpen = false) }
            CameraAction.UseSingleCamera -> _state.update { it.copy(layout = CaptureLayout.SINGLE, isDualUnavailableOpen = false) }
            CameraAction.DismissDualUnavailable -> _state.update { it.copy(isDualUnavailableOpen = false) }
            CameraAction.OpenSettings -> emit(CameraEffect.NavigateToSettings)
            CameraAction.OpenLastCapture -> _state.value.lastCapture?.let { emit(CameraEffect.OpenPhoto(it)) }
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
            else -> takePhoto()
        }
    }

    // Captures every camera on screen: one file in Single, back + front files in the dual layouts.
    private fun takePhoto() {
        val composition = _state.value.photoComposition()
        val keepOriginals = settingsRepository.settings.value.keepOriginals
        emit(CameraEffect.Shutter)
        viewModelScope.launch {
            camera
                .takePhoto(composition, keepOriginals)
                .onSuccess { files -> _state.update { it.copy(lastCapture = files.firstOrNull() ?: it.lastCapture) } }
        }
    }

    private fun startRecording() {
        recordingStarted = false
        _state.update { it.copy(isRecording = true, elapsed = Duration.ZERO) }
        camera.startRecording(_state.value.videoComposition(), settingsRepository.settings.value.keepOriginals)
    }

    private fun stopRecording() {
        if (!_state.value.isRecording) return
        recordingStarted = false
        _state.update { it.copy(isRecording = false) }
        viewModelScope.launch {
            camera.stopRecording().onSuccess { files -> _state.update { it.copy(lastCapture = files.firstOrNull() ?: it.lastCapture) } }
        }
    }

    private fun updateUnlessLocked(transform: (CameraUiState) -> CameraUiState) {
        if (!_state.value.controlsLocked) _state.update(transform)
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

/** Video call merges both cameras into one video; Separate saves one video per camera. */
private fun CameraUiState.videoComposition(): PhotoComposition? =
    if (layout == CaptureLayout.PIP) PhotoComposition.Pip(mainLens, pipCorner, pipShape) else null

/** Video call and Split save one merged photo that looks like the screen; Single and Separate don't merge. */
private fun CameraUiState.photoComposition(): PhotoComposition? =
    when (layout) {
        CaptureLayout.PIP -> PhotoComposition.Pip(mainLens, pipCorner, pipShape)
        CaptureLayout.SPLIT -> PhotoComposition.Split(mainLens, splitDirection, splitRatio)
        CaptureLayout.SINGLE, CaptureLayout.SEPARATE -> null
    }
