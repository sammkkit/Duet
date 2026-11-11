package com.samkit.duet.feature.camera

import com.samkit.duet.core.model.CaptureLayout
import com.samkit.duet.core.model.CaptureType
import com.samkit.duet.core.model.LENS
import com.samkit.duet.core.model.PipCorner
import com.samkit.duet.core.model.PipShape
import org.jetbrains.compose.resources.StringResource

/** Everything the user can do on the camera screen. The UI only ever sends these to CameraViewModel.onAction. */
sealed interface CameraAction {
    data class SelectLayout(
        val layout: CaptureLayout,
    ) : CameraAction

    data class SelectCaptureType(
        val type: CaptureType,
    ) : CameraAction

    data class SelectSeparateType(
        val lens: LENS,
        val type: CaptureType,
    ) : CameraAction

    data object ShutterClick : CameraAction

    data object SnapBothPhotos : CameraAction

    data object SwapCameras : CameraAction

    data class SelectZoom(
        val level: Float,
    ) : CameraAction

    data object CycleFlash : CameraAction

    data object ToggleSplitDirection : CameraAction

    data class ChangeSplitRatio(
        val ratio: Float,
    ) : CameraAction

    data class ChangePipCorner(
        val corner: PipCorner,
    ) : CameraAction

    data class SelectMainLens(
        val lens: LENS,
    ) : CameraAction

    data class SelectPipShape(
        val shape: PipShape,
    ) : CameraAction

    data object ToggleKeepOriginals : CameraAction

    data object UseSingleCamera : CameraAction

    data object CaptureBackThenFront : CameraAction

    data object DismissDualUnavailable : CameraAction

    data object OpenLayoutPicker : CameraAction

    data object DismissLayoutPicker : CameraAction

    data object OpenSettings : CameraAction

    data object OpenLastCapture : CameraAction
}

/** One-off things the UI must do once (not state to redraw): show a message, navigate. */
sealed interface CameraEffect {
    // A resource, not text: the ViewModel has no Compose context to pick the user's language.
    data class ShowMessage(
        val message: StringResource,
    ) : CameraEffect

    data object NavigateToSettings : CameraEffect

    /** A photo is being taken: blink the preview and buzz, like a real camera. */
    data object Shutter : CameraEffect

    data object NavigateToReview : CameraEffect
}
