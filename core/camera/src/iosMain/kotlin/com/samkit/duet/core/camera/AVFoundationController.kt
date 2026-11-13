@file:OptIn(ExperimentalForeignApi::class)

package com.samkit.duet.core.camera

import com.samkit.duet.core.model.FLASH
import com.samkit.duet.core.model.LENS
import com.samkit.duet.core.model.SavedFile
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureDevicePositionFront
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInWideAngleCamera
import platform.AVFoundation.AVCaptureFileOutput
import platform.AVFoundation.AVCaptureFileOutputRecordingDelegateProtocol
import platform.AVFoundation.AVCaptureFlashModeAuto
import platform.AVFoundation.AVCaptureFlashModeOff
import platform.AVFoundation.AVCaptureFlashModeOn
import platform.AVFoundation.AVCaptureInputPort
import platform.AVFoundation.AVCaptureMovieFileOutput
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
import platform.AVFoundation.AVMediaTypeAudio
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVVideoCodecKey
import platform.AVFoundation.AVVideoCodecTypeJPEG
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.defaultDeviceWithDeviceType
import platform.AVFoundation.fileDataRepresentation
import platform.AVFoundation.maxAvailableVideoZoomFactor
import platform.AVFoundation.minAvailableVideoZoomFactor
import platform.AVFoundation.videoZoomFactor
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSLocale
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.darwin.NSObject
import platform.darwin.NSObjectProtocol
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.TimeSource

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

    // Dual layouts: a multi-cam session can't afford photo and movie outputs on both cameras, so one or the other.
    private var videoMode = false
    private var micInput: AVCaptureDeviceInput? = null
    private var active: ActiveRecording? = null
    private var timerJob: Job? = null

    /** Everything one press of the shutter started: one movie per camera, merged on stop for PiP. */
    private class ActiveRecording(
        val outputs: Map<LENS, AVCaptureMovieFileOutput>,
        val results: Map<LENS, CompletableDeferred<NSURL?>>,
        val delegates: List<RecordingDelegate>, // AVFoundation doesn't keep them alive
        val pip: PhotoComposition.Pip?,
        val keepOriginals: Boolean,
        val name: String,
    )

    // AVCapturePhotoOutput doesn't keep its delegate alive, so in-flight captures are held here.
    private val inFlight = mutableSetOf<PhotoDelegate>()

    private data class Bound(
        val device: AVCaptureDevice,
        val photoOutput: AVCapturePhotoOutput?,
        val port: AVCaptureInputPort,
        val movieOutput: AVCaptureMovieFileOutput? = null,
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
            val targets = withContext(Dispatchers.Main) { bound.mapNotNull { (lens, b) -> b.photoOutput?.let { lens to it } }.toMap() }
            check(targets.isNotEmpty()) { "No camera is bound for photos" }
            withContext(Dispatchers.Main) { recentPhotos.clear() }
            val name = "IMG_${timestamp()}"
            val shots =
                coroutineScope {
                    targets.map { (lens, output) -> async { lens to output.capture() } }.awaitAll().toMap()
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

    override fun setVideoMode(enabled: Boolean) {
        if (videoMode == enabled) return
        videoMode = enabled
        if (layers.size > 1) scheduleRebind() // a single camera already has photo and movie outputs
    }

    override fun startRecording(
        composition: PhotoComposition?,
        keepOriginals: Boolean,
    ) {
        if (active != null) return
        val outputs = bound.mapNotNull { (lens, b) -> b.movieOutput?.let { lens to it } }.toMap()
        if (outputs.isEmpty()) return
        val name = "VID_${timestamp()}"
        val pip = (composition as? PhotoComposition.Pip)?.takeIf { outputs.size == 2 }
        // One microphone, one movie with sound: the main camera's (the back camera in Separate).
        session?.connectMic(outputs.getValue(pip?.main ?: outputs.keys.first()))
        val results = outputs.mapValues { CompletableDeferred<NSURL?>() }
        val delegates =
            outputs.map { (lens, output) ->
                // An "error" can still leave a playable movie (e.g. stopped by an interruption): only a missing file fails.
                RecordingDelegate { file ->
                    scope.launch {
                        results.getValue(lens).complete(file.takeIf { NSFileManager.defaultManager.fileExistsAtPath(it.path.orEmpty()) })
                        if (results.values.all { it.isCompleted }) {
                            timerJob?.cancel()
                            _state.value = if (session?.running == true) CameraState.Ready else CameraState.Idle
                        }
                    }
                }.also {
                    output.startRecordingToOutputFileURL(
                        NSURL.fileURLWithPath(
                            NSTemporaryDirectory() + "${name}_${lens.name.lowercase()}.mov",
                        ),
                        it,
                    )
                }
            }
        active = ActiveRecording(outputs, results, delegates, pip, keepOriginals, name)
        val start = TimeSource.Monotonic.markNow()
        timerJob =
            scope.launch {
                while (isActive) {
                    _state.value = CameraState.Recording(start.elapsedNow())
                    delay(500)
                }
            }
    }

    override suspend fun stopRecording(): Result<List<SavedFile>> {
        val recording = active ?: return Result.failure(IllegalStateException("Not recording"))
        withContext(Dispatchers.Main) { recording.outputs.values.forEach { it.stopRecording() } }
        val files = recording.results.mapValues { it.value.await() }
        active = null
        return runCatching {
            try {
                val recorded = files.mapNotNull { (lens, file) -> file?.let { lens to it } }.toMap()
                check(recorded.isNotEmpty()) { "Recording failed" }
                val pip = recording.pip
                if (pip == null) {
                    recorded.map { (lens, file) ->
                        saveVideoToPhotos(
                            file,
                            if (recorded.size >
                                1
                            ) {
                                "${recording.name}_${lens.name.lowercase()}"
                            } else {
                                recording.name
                            },
                        )
                    }
                } else {
                    check(recorded.size == 2) { "A camera stopped recording" }
                    val merged = NSURL.fileURLWithPath(NSTemporaryDirectory() + "${recording.name}.mov")
                    try {
                        composeVideo(recorded, pip, merged)
                        val saved = mutableListOf(saveVideoToPhotos(merged, recording.name))
                        if (recording.keepOriginals) {
                            recorded.forEach { (lens, file) ->
                                saved +=
                                    saveVideoToPhotos(file, "${recording.name}_${lens.name.lowercase()}")
                            }
                        }
                        saved
                    } finally {
                        NSFileManager.defaultManager.removeItemAtURL(merged, null)
                    }
                }
            } finally {
                files.values.filterNotNull().forEach { NSFileManager.defaultManager.removeItemAtURL(it, null) } // Photos has its own copy
            }
        }
    }

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
        micInput = null
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
        val single = layers.size == 1
        val ok =
            layers.all { (lens, layer) ->
                new.connect(lens, devices.getValue(lens)!!, layer, photo = single || !videoMode)?.also { bound[lens] = it } != null
            }
        if (ok && (single || videoMode)) {
            for (lens in bound.keys.toList()) {
                bound[lens] =
                    bound.getValue(lens).let { it.copy(movieOutput = new.connectMovieOutput(it.port)) }
            }
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

    /** camera -> preview layer and (if [photo]) camera -> photo output, as explicit connections. Null if the hardware refuses. */
    private fun AVCaptureSession.connect(
        lens: LENS,
        device: AVCaptureDevice,
        layer: AVCaptureVideoPreviewLayer,
        photo: Boolean,
    ): Bound? {
        val input = AVCaptureDeviceInput.deviceInputWithDevice(device, null) ?: return null
        if (!canAddInput(input)) return null
        addInputWithNoConnections(input)
        val port = input.ports.filterIsInstance<AVCaptureInputPort>().firstOrNull { it.mediaType == AVMediaTypeVideo } ?: return null

        layer.setSessionWithNoConnection(this)
        val previewConnection = AVCaptureConnection(inputPort = port, videoPreviewLayer = layer)
        if (!canAddConnection(previewConnection)) return null
        addConnection(previewConnection)

        var photoOutput: AVCapturePhotoOutput? = null
        if (photo) {
            val output = AVCapturePhotoOutput()
            if (!canAddOutput(output)) return null
            addOutputWithNoConnections(output)
            val photoConnection = AVCaptureConnection(inputPorts = listOf(port), output = output)
            if (!canAddConnection(photoConnection)) return null
            addConnection(photoConnection)
            if (photoConnection.isVideoRotationAngleSupported(90.0)) photoConnection.videoRotationAngle = 90.0
            photoOutput = output
        }

        // Sensors are landscape; 90° = portrait, the only orientation the app uses.
        if (previewConnection.isVideoRotationAngleSupported(90.0)) previewConnection.videoRotationAngle = 90.0
        // Selfie preview mirrored like a mirror; the saved photo isn't (same as Android).
        if (lens == LENS.FRONT && previewConnection.isVideoMirroringSupported()) {
            previewConnection.automaticallyAdjustsVideoMirroring = false
            previewConnection.videoMirrored = true
        }
        return Bound(device, photoOutput, port)
    }

    /** camera -> movie file. Null if this session can't take one more output. */
    private fun AVCaptureSession.connectMovieOutput(port: AVCaptureInputPort): AVCaptureMovieFileOutput? {
        val output = AVCaptureMovieFileOutput()
        if (!canAddOutput(output)) return null
        addOutputWithNoConnections(output)
        val connection = AVCaptureConnection(inputPorts = listOf(port), output = output)
        if (!canAddConnection(connection)) return null
        addConnection(connection)
        if (connection.isVideoRotationAngleSupported(90.0)) connection.videoRotationAngle = 90.0
        return output
    }

    // Added when recording starts, not at bind: the mic permission may have been granted after the camera opened.
    private fun AVCaptureSession.connectMic(output: AVCaptureMovieFileOutput) {
        if (micInput != null) return
        if (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeAudio) != AVAuthorizationStatusAuthorized) return
        val mic = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeAudio) ?: return
        val input = AVCaptureDeviceInput.deviceInputWithDevice(mic, null) ?: return
        beginConfiguration()
        if (canAddInput(input)) {
            addInputWithNoConnections(input)
            val port = input.ports.filterIsInstance<AVCaptureInputPort>().firstOrNull { it.mediaType == AVMediaTypeAudio }
            val connection = port?.let { AVCaptureConnection(inputPorts = listOf(it), output = output) }
            if (connection != null && canAddConnection(connection)) addConnection(connection)
            micInput = input
        }
        commitConfiguration()
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

private class RecordingDelegate(
    private val onDone: (NSURL) -> Unit,
) : NSObject(),
    AVCaptureFileOutputRecordingDelegateProtocol {
    override fun captureOutput(
        output: AVCaptureFileOutput,
        didFinishRecordingToOutputFileAtURL: NSURL,
        fromConnections: List<*>,
        error: NSError?,
    ) {
        onDone(didFinishRecordingToOutputFileAtURL)
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
