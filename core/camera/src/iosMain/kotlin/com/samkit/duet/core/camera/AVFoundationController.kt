@file:OptIn(ExperimentalForeignApi::class)

package com.samkit.duet.core.camera

import com.samkit.duet.core.model.FLASH
import com.samkit.duet.core.model.LENS
import com.samkit.duet.core.model.SavedFile
import kotlinx.cinterop.ExperimentalForeignApi
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureDevicePositionFront
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInWideAngleCamera
import platform.AVFoundation.AVCaptureFlashModeAuto
import platform.AVFoundation.AVCaptureFlashModeOff
import platform.AVFoundation.AVCaptureFlashModeOn
import platform.AVFoundation.AVCaptureInputPort
import platform.AVFoundation.AVCaptureMultiCamSession
import platform.AVFoundation.AVCapturePhoto
import platform.AVFoundation.AVCapturePhotoCaptureDelegateProtocol
import platform.AVFoundation.AVCapturePhotoOutput
import platform.AVFoundation.AVCapturePhotoSettings
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionInterruptionEndedNotification
import platform.AVFoundation.AVCaptureSessionInterruptionReasonKey
import platform.AVFoundation.AVCaptureSessionInterruptionReasonVideoDeviceInUseByAnotherClient
import platform.AVFoundation.AVCaptureSessionRuntimeErrorNotification
import platform.AVFoundation.AVCaptureSessionWasInterruptedNotification
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVVideoCodecKey
import platform.AVFoundation.AVVideoCodecTypeJPEG
import platform.AVFoundation.defaultDeviceWithDeviceType
import platform.AVFoundation.fileDataRepresentation
import platform.AVFoundation.maxAvailableVideoZoomFactor
import platform.AVFoundation.minAvailableVideoZoomFactor
import platform.AVFoundation.videoZoomFactor
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSError
import platform.Foundation.NSLocale
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.Foundation.NSOperationQueue
import platform.darwin.NSObject
import platform.darwin.NSObjectProtocol
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * CameraController on top of AVFoundation, mirroring CameraXController.
 *
 * Every CameraPreview [attach]es its lens. One lens = a normal AVCaptureSession, two = AVCaptureMultiCamSession.
 * Both are wired by hand (inputs and outputs "with no connections" + explicit AVCaptureConnections), because
 * a multi-cam session can't guess which camera goes to which preview; the single session just reuses that code.
 */
class AVFoundationController : CameraController {
    private val _state = MutableStateFlow<CameraState>(CameraState.Idle)
    override val state: StateFlow<CameraState> = _state.asStateFlow()

    // Main (not immediate): both PiP previews attach before the rebind runs, so it configures once.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val rebindLock = Mutex()
    private var rebindJob: Job? = null

    private val layers = linkedMapOf<LENS, AVCaptureVideoPreviewLayer>()
    private val bound = linkedMapOf<LENS, Bound>()
    private var session: AVCaptureSession? = null
    private var observers = emptyList<NSObjectProtocol>()
    private var zoom = 1f
    private var flash = FLASH.OFF

    // AVCapturePhotoOutput doesn't keep its delegate alive, so in-flight captures are held here.
    private val inFlight = mutableSetOf<PhotoDelegate>()

    private data class Bound(
        val device: AVCaptureDevice,
        val photoOutput: AVCapturePhotoOutput,
    )

    /** iOS-only: a CameraPreview showing [lens] appeared. */
    fun attach(
        lens: LENS,
        layer: AVCaptureVideoPreviewLayer,
    ) {
        layers[lens] = layer
        scheduleRebind()
    }

    /** iOS-only: that CameraPreview left. Ignored if another preview already took over this lens (e.g. on swap). */
    fun detach(
        lens: LENS,
        layer: AVCaptureVideoPreviewLayer,
    ) {
        if (layers[lens] !== layer) return
        layers.remove(lens)
        scheduleRebind()
    }

    override suspend fun supportsDualCamera(): Boolean = AVCaptureMultiCamSession.multiCamSupported

    override suspend fun takePhoto(
        composition: PhotoComposition?,
        keepOriginals: Boolean,
    ): Result<List<SavedFile>> =
        runCatching {
            val targets = withContext(Dispatchers.Main) { bound.toMap() }
            check(targets.isNotEmpty()) { "No camera is bound" }
            val name = "IMG_${timestamp()}"
            val shots =
                coroutineScope {
                    targets.map { (lens, b) -> async { lens to b.photoOutput.capture() } }.awaitAll().toMap()
                }
            if (composition != null && shots.size == 2) {
                val merged = withContext(Dispatchers.Default) { composePhoto(shots, composition) }
                val originals =
                    if (keepOriginals) {
                        shots.map { (lens, data) ->
                            saveToPhotos(data, "${name}_${lens.name.lowercase()}")
                        }
                    } else {
                        emptyList()
                    }
                listOf(saveToPhotos(merged, name)) + originals
            } else {
                shots.map { (lens, data) -> saveToPhotos(data, if (shots.size > 1) "${name}_${lens.name.lowercase()}" else name) }
            }
        }

    override fun startRecording(): Unit = TODO("#13 video recording")

    override suspend fun stopRecording(): Result<SavedFile> = TODO("#13 video recording")

    override fun setZoom(ratio: Float) {
        zoom = ratio
        applyZoom()
    }

    override fun setFlash(mode: FLASH) {
        flash = mode // read at capture time: AVFoundation sets flash per photo
    }

    private fun scheduleRebind() {
        rebindJob?.cancel()
        rebindJob = scope.launch { rebindLock.withLock { rebind() } }
    }

    private suspend fun rebind() {
        val old = session
        session = null
        bound.clear()
        observers.forEach { NSNotificationCenter.defaultCenter.removeObserver(it) }
        observers = emptyList()
        // start/stopRunning block for a few hundred ms: keep them off the main thread.
        old?.let { withContext(Dispatchers.Default) { it.stopRunning() } }

        if (layers.isEmpty()) {
            _state.value = CameraState.Idle
            return
        }
        val devices = layers.keys.associateWith { it.device() }
        if (devices.values.any { it == null }) {
            _state.value = CameraState.Error(CameraError.NO_CAMERA)
            return
        }

        val new = if (layers.size > 1) AVCaptureMultiCamSession() else AVCaptureSession()
        new.beginConfiguration()
        val ok =
            layers.all { (lens, layer) ->
                new.connect(lens, devices.getValue(lens)!!, layer)?.also { bound[lens] = it } != null
            }
        new.commitConfiguration()
        if (!ok) {
            bound.clear()
            _state.value = CameraState.Error(CameraError.UNKNOWN)
            return
        }

        session = new
        observers = observe(new)
        applyZoom()
        withContext(Dispatchers.Default) { new.startRunning() }
        _state.value = if (new.running) CameraState.Ready else CameraState.Error(CameraError.UNKNOWN)
    }

    /** camera -> preview layer and camera -> photo output, as explicit connections. Null if the hardware refuses. */
    private fun AVCaptureSession.connect(
        lens: LENS,
        device: AVCaptureDevice,
        layer: AVCaptureVideoPreviewLayer,
    ): Bound? {
        val input = AVCaptureDeviceInput.deviceInputWithDevice(device, null) ?: return null
        if (!canAddInput(input)) return null
        addInputWithNoConnections(input)
        val port = input.ports.filterIsInstance<AVCaptureInputPort>().firstOrNull { it.mediaType == AVMediaTypeVideo } ?: return null

        layer.setSessionWithNoConnection(this)
        val previewConnection = AVCaptureConnection(inputPort = port, videoPreviewLayer = layer)
        if (!canAddConnection(previewConnection)) return null
        addConnection(previewConnection)

        val photoOutput = AVCapturePhotoOutput()
        if (!canAddOutput(photoOutput)) return null
        addOutputWithNoConnections(photoOutput)
        val photoConnection = AVCaptureConnection(inputPorts = listOf(port), output = photoOutput)
        if (!canAddConnection(photoConnection)) return null
        addConnection(photoConnection)

        // Sensors are landscape; 90° = portrait, the only orientation the app uses.
        listOf(previewConnection, photoConnection).forEach { if (it.isVideoRotationAngleSupported(90.0)) it.videoRotationAngle = 90.0 }
        // Selfie preview mirrored like a mirror; the saved photo isn't (same as Android).
        if (lens == LENS.FRONT && previewConnection.isVideoMirroringSupported()) {
            previewConnection.automaticallyAdjustsVideoMirroring = false
            previewConnection.videoMirrored = true
        }
        return Bound(device, photoOutput)
    }

    // Interruptions are iOS's version of CameraX's camera state: another app took the camera, a call came in, etc.
    private fun observe(session: AVCaptureSession): List<NSObjectProtocol> {
        val center = NSNotificationCenter.defaultCenter
        val queue = NSOperationQueue.mainQueue
        return listOf(
            center.addObserverForName(AVCaptureSessionWasInterruptedNotification, session, queue) { note ->
                val reason = (note?.userInfo?.get(AVCaptureSessionInterruptionReasonKey) as? NSNumber)?.longValue
                _state.value =
                    if (reason == AVCaptureSessionInterruptionReasonVideoDeviceInUseByAnotherClient) {
                        CameraState.Error(CameraError.CAMERA_IN_USE)
                    } else {
                        CameraState.Idle // e.g. app in background: it resumes by itself
                    }
            },
            center.addObserverForName(AVCaptureSessionInterruptionEndedNotification, session, queue) { _ ->
                _state.value = CameraState.Ready
            },
            center.addObserverForName(AVCaptureSessionRuntimeErrorNotification, session, queue) { _ ->
                _state.value = CameraState.Error(CameraError.UNKNOWN)
            },
        )
    }

    private fun applyZoom() {
        bound.values.forEach {
            (it.device to zoom.toDouble()).let { (device, ratio) ->
                if (device.lockForConfiguration(null)) {
                    device.videoZoomFactor = ratio.coerceIn(device.minAvailableVideoZoomFactor, device.maxAvailableVideoZoomFactor)
                    device.unlockForConfiguration()
                }
            }
        }
    }

    private suspend fun AVCapturePhotoOutput.capture(): NSData =
        suspendCancellableCoroutine { continuation ->
            val settings = AVCapturePhotoSettings.photoSettingsWithFormat(mapOf<Any?, Any?>(AVVideoCodecKey to AVVideoCodecTypeJPEG))
            val mode = flash.toFlashMode()
            // Asking for a flash mode the camera lacks (front camera) throws, so only set supported ones.
            if (supportedFlashModes.any { (it as? NSNumber)?.longValue == mode }) settings.flashMode = mode
            lateinit var delegate: PhotoDelegate
            delegate =
                PhotoDelegate { data, error ->
                    inFlight -= delegate
                    if (data != null) {
                        continuation.resume(data)
                    } else {
                        continuation.resumeWithException(IllegalStateException(error?.localizedDescription ?: "No photo data"))
                    }
                }
            inFlight += delegate
            capturePhotoWithSettings(settings, delegate)
        }
}

private class PhotoDelegate(
    private val onDone: (NSData?, NSError?) -> Unit,
) : NSObject(),
    AVCapturePhotoCaptureDelegateProtocol {
    override fun captureOutput(
        output: AVCapturePhotoOutput,
        didFinishProcessingPhoto: AVCapturePhoto,
        error: NSError?,
    ) {
        onDone(if (error == null) didFinishProcessingPhoto.fileDataRepresentation() else null, error)
    }
}

private fun LENS.device(): AVCaptureDevice? =
    AVCaptureDevice.defaultDeviceWithDeviceType(
        AVCaptureDeviceTypeBuiltInWideAngleCamera,
        AVMediaTypeVideo,
        if (this == LENS.BACK) AVCaptureDevicePositionBack else AVCaptureDevicePositionFront,
    )

private fun FLASH.toFlashMode() =
    when (this) {
        FLASH.AUTO -> AVCaptureFlashModeAuto
        FLASH.ON -> AVCaptureFlashModeOn
        FLASH.OFF -> AVCaptureFlashModeOff
    }

// TODO(#11): name comes from core:media FileNaming.
private fun timestamp(): String =
    NSDateFormatter()
        .apply {
            dateFormat = "yyyyMMdd_HHmmss"
            locale = NSLocale(localeIdentifier = "en_US_POSIX")
        }.stringFromDate(NSDate())
