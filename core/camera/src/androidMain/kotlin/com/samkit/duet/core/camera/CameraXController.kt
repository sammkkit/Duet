package com.samkit.duet.core.camera

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ConcurrentCamera.SingleCameraConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import com.samkit.duet.core.model.FLASH
import com.samkit.duet.core.model.LENS
import com.samkit.duet.core.model.SavedFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import androidx.camera.core.CameraState as XCameraState

/**
 * CameraController on top of CameraX.
 *
 * Binding follows what's on screen: every CameraPreview [attach]es its lens. One lens attached = single camera,
 * two = both cameras at once (CameraX concurrent camera, non-composition mode: own preview + photo per camera).
 */
class CameraXController(
    private val context: Context,
) : CameraController {
    private val _state = MutableStateFlow<CameraState>(CameraState.Idle)
    override val state: StateFlow<CameraState> = _state.asStateFlow()

    // Main, not Main.immediate: attaches from the same frame land before the rebind runs, so PiP binds once, not twice.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var rebindJob: Job? = null

    private val slots = linkedMapOf<LENS, Slot>()
    private val bound = linkedMapOf<LENS, Camera>()
    private val cameraStates = mutableMapOf<LENS, CameraState>()
    private var owner: LifecycleOwner? = null
    private var provider: ProcessCameraProvider? = null
    private var zoom = 1f
    private var flash = FLASH.OFF

    /** One camera's use cases, tied to the preview surface showing it. */
    private class Slot(
        val surfaceProvider: Preview.SurfaceProvider,
    ) {
        val preview = Preview.Builder().build().also { it.surfaceProvider = surfaceProvider }
        val imageCapture = ImageCapture.Builder().build()
    }

    /** Android-only: a CameraPreview showing [lens] appeared. */
    fun attach(
        lens: LENS,
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
    ) {
        owner = lifecycleOwner
        slots[lens] = Slot(surfaceProvider)
        scheduleRebind()
    }

    /** Android-only: that CameraPreview left. Ignored if another preview already took over this lens (e.g. on swap). */
    fun detach(
        lens: LENS,
        surfaceProvider: Preview.SurfaceProvider,
    ) {
        if (slots[lens]?.surfaceProvider !== surfaceProvider) return
        slots.remove(lens)
        scheduleRebind()
    }

    override suspend fun supportsDualCamera(): Boolean =
        withContext(Dispatchers.Main) {
            cameraProvider().availableConcurrentCameraInfos.any { combo ->
                combo.any { it.lensFacing == CameraSelector.LENS_FACING_BACK } &&
                    combo.any { it.lensFacing == CameraSelector.LENS_FACING_FRONT }
            }
        }

    override suspend fun takePhoto(
        composition: PhotoComposition?,
        keepOriginals: Boolean,
    ): Result<List<SavedFile>> =
        runCatching {
            val targets = withContext(Dispatchers.Main) { slots.filterKeys { it in bound } }
            check(targets.isNotEmpty()) { "No camera is bound" }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            if (composition != null && targets.size == 2) {
                takeComposedPhoto(targets, composition, keepOriginals, "IMG_$stamp")
            } else {
                coroutineScope {
                    targets
                        .map { (lens, slot) ->
                            val suffix = if (targets.size > 1) "_${lens.name.lowercase()}" else ""
                            async { slot.imageCapture.saveToGallery("IMG_$stamp$suffix") }
                        }.awaitAll()
                }
            }
        }

    // Both cameras into memory at once, merged like the screen shows them, saved as one photo.
    private suspend fun takeComposedPhoto(
        targets: Map<LENS, Slot>,
        composition: PhotoComposition,
        keepOriginals: Boolean,
        name: String,
    ): List<SavedFile> {
        val shots =
            coroutineScope {
                targets
                    .map { (lens, slot) ->
                        async { lens to slot.imageCapture.captureBitmap() }
                    }.awaitAll()
                    .toMap()
            }
        try {
            val merged = withContext(Dispatchers.Default) { composePhoto(shots, composition) }
            val files = mutableListOf(context.saveBitmapToGallery(merged, name))
            merged.recycle()
            if (keepOriginals) {
                shots.forEach { (lens, bitmap) ->
                    files +=
                        context.saveBitmapToGallery(bitmap, "${name}_${lens.name.lowercase()}")
                }
            }
            return files
        } finally {
            shots.values.forEach { it.recycle() }
        }
    }

    override fun startRecording(): Unit = TODO("#13 video recording")

    override suspend fun stopRecording(): Result<SavedFile> = TODO("#13 video recording")

    override fun setZoom(ratio: Float) {
        zoom = ratio
        applyZoom()
    }

    override fun setFlash(mode: FLASH) {
        flash = mode
        applyFlash()
    }

    private fun scheduleRebind() {
        rebindJob?.cancel()
        rebindJob = scope.launch { rebind() }
    }

    private suspend fun rebind() {
        val provider = cameraProvider()
        val owner = owner
        bound.values.forEach { camera -> owner?.let { camera.cameraInfo.cameraState.removeObservers(it) } }
        provider.unbindAll()
        bound.clear()
        cameraStates.clear()

        if (owner == null || slots.isEmpty()) {
            this.owner = null // drop the Activity reference: nothing on screen uses the camera
            _state.value = CameraState.Idle
            return
        }
        if (slots.keys.any { !provider.hasCamera(it.toSelector()) }) {
            _state.value = CameraState.Error(CameraError.NO_CAMERA)
            return
        }

        runCatching {
            if (slots.size == 1) {
                val (lens, slot) = slots.entries.first()
                bound[lens] = provider.bindToLifecycle(owner, lens.toSelector(), slot.preview, slot.imageCapture)
            } else {
                val configs =
                    slots.map { (lens, slot) ->
                        val group =
                            UseCaseGroup
                                .Builder()
                                .addUseCase(slot.preview)
                                .addUseCase(slot.imageCapture)
                                .build()
                        SingleCameraConfig(lens.toSelector(), group, owner)
                    }
                slots.keys.zip(provider.bindToLifecycle(configs).cameras).forEach { (lens, camera) -> bound[lens] = camera }
            }
        }.onFailure {
            _state.value = CameraState.Error(CameraError.UNKNOWN)
            return
        }

        applyZoom()
        applyFlash()
        // Hardware events (opening, open, closed, taken by another app) arrive as LiveData updates.
        bound.forEach { (lens, camera) ->
            camera.cameraInfo.cameraState.observe(owner) { xState ->
                cameraStates[lens] = xState.toCameraState()
                _state.value = combinedState()
            }
        }
    }

    private fun combinedState(): CameraState =
        cameraStates.values.firstOrNull { it is CameraState.Error }
            ?: if (cameraStates.size == bound.size && cameraStates.values.all { it == CameraState.Ready }) {
                CameraState.Ready
            } else {
                CameraState.Idle
            }

    private fun applyZoom() {
        bound.values.forEach { camera ->
            val range = camera.cameraInfo.zoomState.value ?: return@forEach
            camera.cameraControl.setZoomRatio(zoom.coerceIn(range.minZoomRatio, range.maxZoomRatio))
        }
    }

    private fun applyFlash() {
        slots.values.forEach { it.imageCapture.flashMode = flash.toFlashMode() }
    }

    private suspend fun cameraProvider(): ProcessCameraProvider =
        provider ?: ProcessCameraProvider.getInstance(context).await().also { provider = it }

    // TODO(#11): name comes from core:media FileNaming; Android 9 and lower also need WRITE_EXTERNAL_STORAGE at runtime.
    private suspend fun ImageCapture.saveToGallery(name: String): SavedFile =
        suspendCancellableCoroutine { continuation ->
            val values =
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.jpg")
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/DualCam")
                }
            val options =
                ImageCapture.OutputFileOptions
                    .Builder(context.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    .build()
            takePicture(
                options,
                DirectExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        continuation.resume(SavedFile(output.savedUri.toString()))
                    }

                    override fun onError(exception: ImageCaptureException) {
                        continuation.resumeWithException(exception)
                    }
                },
            )
        }
}

private fun LENS.toSelector(): CameraSelector =
    when (this) {
        LENS.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
        LENS.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
    }

private fun FLASH.toFlashMode(): Int =
    when (this) {
        FLASH.AUTO -> ImageCapture.FLASH_MODE_AUTO
        FLASH.ON -> ImageCapture.FLASH_MODE_ON
        FLASH.OFF -> ImageCapture.FLASH_MODE_OFF
    }

private fun XCameraState.toCameraState(): CameraState {
    val error = error
    return when {
        error != null ->
            CameraState.Error(
                when (error.code) {
                    XCameraState.ERROR_CAMERA_IN_USE, XCameraState.ERROR_MAX_CAMERAS_IN_USE -> CameraError.CAMERA_IN_USE
                    else -> CameraError.UNKNOWN
                },
            )
        type == XCameraState.Type.OPEN -> CameraState.Ready
        else -> CameraState.Idle
    }
}
