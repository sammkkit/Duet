package com.samkit.duet.feature.camera

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.samkit.duet.core.camera.rememberPhotoViewer
import com.samkit.duet.core.designsystem.theme.DuetTheme
import com.samkit.duet.core.model.CaptureLayout
import com.samkit.duet.core.model.CaptureType
import com.samkit.duet.core.permissions.Permission
import com.samkit.duet.core.permissions.PermissionState
import com.samkit.duet.core.permissions.rememberPermissionState
import com.samkit.duet.feature.camera.layouts.PipLayout
import com.samkit.duet.feature.camera.layouts.SeparateLayout
import com.samkit.duet.feature.camera.layouts.SingleLayout
import com.samkit.duet.feature.camera.layouts.SplitLayout
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.seconds

/**
 * Stateful entry point: owns the ViewModel, turns effects into snackbars and navigation.
 * Everything visual lives in [CameraScreen], which only takes plain state + an action callback.
 */
@Composable
fun CameraRoute(
    onNavigateToSettings: () -> Unit = {},
    viewModel: CameraViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    // The effect collector outlives recompositions; read the latest callbacks, not the ones from the first frame.
    val toSettings by rememberUpdatedState(onNavigateToSettings)
    // Asked when Video is picked, not at launch: photo-only users never see a mic prompt. Denied = silent video.
    val mic = rememberPermissionState(Permission.MICROPHONE)
    LaunchedEffect(state.captureType) {
        if (state.captureType == CaptureType.VIDEO && mic.state == PermissionState.Denied) mic.request()
    }

    val haptics = LocalHapticFeedback.current
    val openPhoto by rememberUpdatedState(rememberPhotoViewer())
    val shutterFlash = remember { Animatable(0f) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        // Only handle effects while visible; ones sent in the background wait in the channel.
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    CameraEffect.NavigateToSettings -> toSettings()
                    CameraEffect.Shutter -> {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        launch {
                            shutterFlash.snapTo(1f)
                            shutterFlash.animateTo(0f, tween(durationMillis = 250))
                        }
                    }
                    is CameraEffect.OpenPhoto -> openPhoto(effect.file)
                }
            }
        }
    }

    CameraScreen(
        state = state,
        onAction = viewModel::onAction,
        keepOriginals = settings.keepOriginals,
        shutterFlash = { shutterFlash.value },
    )
}

/** Stateless: draws [state], reports every tap as a [CameraAction]. Previewable and testable without a ViewModel. */
@Composable
fun CameraScreen(
    state: CameraUiState,
    onAction: (CameraAction) -> Unit,
    modifier: Modifier = Modifier,
    keepOriginals: Boolean = true,
    shutterFlash: () -> Float = { 0f },
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets)) {
            CameraTopBar(
                flash = state.flash,
                showLayoutButton = state.layout != CaptureLayout.SINGLE,
                onAction = onAction,
            ) {
                TopBarCenter(state, onAction)
            }

            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp)) {
                val previewModifier = Modifier.fillMaxSize()
                when (state.layout) {
                    CaptureLayout.SINGLE -> SingleLayout(state, onAction, previewModifier)
                    CaptureLayout.PIP -> PipLayout(state, onAction, previewModifier)
                    CaptureLayout.SPLIT -> SplitLayout(state, onAction, previewModifier)
                    CaptureLayout.SEPARATE -> SeparateLayout(state, onAction, previewModifier)
                }
                // Shutter blink. A lambda read in graphicsLayer: animating it redraws, it doesn't recompose.
                Box(
                    Modifier
                        .matchParentSize()
                        .graphicsLayer { alpha = shutterFlash() }
                        .background(Color.Black),
                )
            }

            CameraBottomBar(state = state, onAction = onAction)
        }
    }

    // Overlays are driven by state like everything else: the ViewModel decides when they show.
    if (state.isLayoutPickerOpen) LayoutPickerSheet(state, keepOriginals, onAction)
    if (state.isDualUnavailableOpen) DualUnavailableDialog(onAction)
}

@Composable
private fun ScreenPreview(state: CameraUiState) {
    DuetTheme { CameraScreen(state = state, onAction = {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun SinglePreview() = ScreenPreview(CameraUiState())

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun PipRecordingPreview() =
    ScreenPreview(CameraUiState(layout = CaptureLayout.PIP, captureType = CaptureType.VIDEO, isRecording = true, elapsed = 42.seconds))

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun SplitPreview() = ScreenPreview(CameraUiState(layout = CaptureLayout.SPLIT))

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun SeparatePreview() = ScreenPreview(CameraUiState(layout = CaptureLayout.SEPARATE, isRecording = true, elapsed = 72.seconds))
