@file:OptIn(ExperimentalForeignApi::class)

package com.samkit.duet.core.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.samkit.duet.core.model.LENS
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import org.koin.compose.koinInject
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.CoreGraphics.CGRectZero
import platform.QuartzCore.CATransaction
import platform.UIKit.UIColor
import platform.UIKit.UIView

@Composable
actual fun CameraPreview(
    lens: LENS,
    modifier: Modifier,
) {
    val controller = koinInject<AVFoundationController>()
    val view = remember { PreviewView() }

    // Appearing on screen is what connects this lens; leaving releases it (same as Android).
    DisposableEffect(lens, view) {
        controller.attach(lens, view.previewLayer)
        onDispose { controller.detach(lens, view.previewLayer) }
    }

    // Not interactive: taps and drags (swap, move the PiP window) go to Compose, not to the UIView.
    UIKitView(
        factory = { view },
        modifier = modifier,
        properties = UIKitInteropProperties(interactionMode = null),
    )
}

/** A UIView whose preview layer always fills it (layers don't resize with their view on their own). */
private class PreviewView : UIView(frame = CGRectZero.readValue()) {
    val previewLayer = AVCaptureVideoPreviewLayer().apply { videoGravity = AVLayerVideoGravityResizeAspectFill }

    init {
        clipsToBounds = true
        backgroundColor = UIColor.blackColor // until the first frame arrives (and always on the simulator: no camera)
        layer.addSublayer(previewLayer)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        CATransaction.begin()
        CATransaction.setDisableActions(true) // no implicit resize animation
        previewLayer.frame = bounds
        CATransaction.commit()
    }
}
