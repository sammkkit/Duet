package com.samkit.duet.core.camera

import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.samkit.duet.core.model.LENS
import org.koin.compose.koinInject

@Composable
actual fun CameraPreview(
    lens: LENS,
    modifier: Modifier,
) {
    val controller = koinInject<CameraXController>()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    // COMPATIBLE = TextureView: it respects Compose clipping (our rounded corners); the default SurfaceView doesn't.
    val previewView =
        remember {
            PreviewView(context).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        }

    // Appearing on screen is what binds this lens; leaving releases it.
    DisposableEffect(lens, lifecycleOwner, previewView) {
        val surfaceProvider = previewView.surfaceProvider
        controller.attach(lens, lifecycleOwner, surfaceProvider)
        onDispose { controller.detach(lens, surfaceProvider) }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}
