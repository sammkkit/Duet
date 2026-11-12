package com.samkit.duet.feature.camera

import com.samkit.duet.core.model.CaptureLayout
import com.samkit.duet.core.model.CaptureType
import com.samkit.duet.core.model.FLASH
import com.samkit.duet.core.model.LENS
import com.samkit.duet.core.model.PipCorner
import com.samkit.duet.core.model.PipShape
import com.samkit.duet.core.model.SavedFile
import com.samkit.duet.core.model.SplitDirection
import kotlin.time.Duration

/** Everything the camera screen shows. Defaults = the screen on first open. */
data class CameraUiState(
    val layout: CaptureLayout = CaptureLayout.SINGLE,
    val captureType: CaptureType = CaptureType.PHOTO,
    val isRecording: Boolean = false,
    val elapsed: Duration = Duration.ZERO,
    val flash: FLASH = FLASH.OFF,
    val zoom: Float = 1f,
    // Single: the camera in use. Video call: the camera in the big view.
    val mainLens: LENS = LENS.BACK,
    val pipCorner: PipCorner = PipCorner.TopEnd,
    val splitRatio: Float = 0.5f,
    val splitDirection: SplitDirection = SplitDirection.TopBottom,
    val pipShape: PipShape = PipShape.ROUNDED,
    val isLayoutPickerOpen: Boolean = false,
    // Shown when a dual layout is picked on a phone that can't run both cameras.
    val isDualUnavailableOpen: Boolean = false,
    // Shown in the bottom-left thumbnail. In PiP/Split: the merged photo.
    val lastCapture: SavedFile? = null,
) {
    /** Layout, capture type and separate-mode types are locked while recording. */
    val controlsLocked: Boolean get() = isRecording

    /** What the shutter does. Split is photo-only for now; the other layouts record video too. */
    val shutterType: CaptureType
        get() = if (layout == CaptureLayout.SPLIT) CaptureType.PHOTO else captureType
}

const val MIN_SPLIT_RATIO = 0.3f
const val MAX_SPLIT_RATIO = 0.7f
