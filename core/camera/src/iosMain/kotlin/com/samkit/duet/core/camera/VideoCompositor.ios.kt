@file:OptIn(ExperimentalForeignApi::class)

package com.samkit.duet.core.camera

import com.samkit.duet.core.model.LENS
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.AVAssetExportPresetHighestQuality
import platform.AVFoundation.AVAssetExportSession
import platform.AVFoundation.AVAssetExportSessionStatusCompleted
import platform.AVFoundation.AVAssetTrack
import platform.AVFoundation.AVFileTypeQuickTimeMovie
import platform.AVFoundation.AVMediaTypeAudio
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMutableComposition
import platform.AVFoundation.AVMutableVideoComposition
import platform.AVFoundation.AVMutableVideoCompositionInstruction
import platform.AVFoundation.AVMutableVideoCompositionLayerInstruction
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.addMutableTrackWithMediaType
import platform.AVFoundation.duration
import platform.AVFoundation.naturalSize
import platform.AVFoundation.preferredTransform
import platform.AVFoundation.setVideoComposition
import platform.AVFoundation.tracksWithMediaType
import platform.CoreGraphics.CGAffineTransformConcat
import platform.CoreGraphics.CGAffineTransformInvert
import platform.CoreGraphics.CGAffineTransformMakeScale
import platform.CoreGraphics.CGAffineTransformMakeTranslation
import platform.CoreGraphics.CGRectApplyAffineTransform
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeApplyAffineTransform
import platform.CoreGraphics.CGSizeMake
import platform.CoreMedia.CMTimeMake
import platform.CoreMedia.CMTimeMinimum
import platform.CoreMedia.CMTimeRangeMake
import platform.CoreMedia.kCMPersistentTrackID_Invalid
import platform.CoreMedia.kCMTimeZero
import platform.Foundation.NSURL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlin.math.max

/**
 * Merges the two PiP recordings into one movie laid out like the screen, using the shared [layout] math.
 * The small camera is cropped to the window and drawn over the main one; sound comes from the main.
 * ponytail: the window is a plain rectangle in the video (rounded/circle needs a custom AVVideoCompositing).
 */
internal suspend fun composeVideo(
    files: Map<LENS, NSURL>,
    pip: PhotoComposition.Pip,
    output: NSURL,
) {
    val mainAsset = AVURLAsset(uRL = files.getValue(pip.main), options = null)
    val smallAsset = AVURLAsset(uRL = files.getValue(if (pip.main == LENS.BACK) LENS.FRONT else LENS.BACK), options = null)
    val mainVideo = mainAsset.tracksWithMediaType(AVMediaTypeVideo).first() as AVAssetTrack
    val smallVideo = smallAsset.tracksWithMediaType(AVMediaTypeVideo).first() as AVAssetTrack
    val range = CMTimeRangeMake(kCMTimeZero.readValue(), CMTimeMinimum(mainAsset.duration, smallAsset.duration))

    val mix = AVMutableComposition()
    val mainTrack = mix.addMutableTrackWithMediaType(AVMediaTypeVideo, kCMPersistentTrackID_Invalid)!!
    mainTrack.insertTimeRange(range, ofTrack = mainVideo, atTime = kCMTimeZero.readValue(), error = null)
    val smallTrack = mix.addMutableTrackWithMediaType(AVMediaTypeVideo, kCMPersistentTrackID_Invalid)!!
    smallTrack.insertTimeRange(range, ofTrack = smallVideo, atTime = kCMTimeZero.readValue(), error = null)
    (mainAsset.tracksWithMediaType(AVMediaTypeAudio).firstOrNull() as AVAssetTrack?)?.let { audio ->
        mix
            .addMutableTrackWithMediaType(AVMediaTypeAudio, kCMPersistentTrackID_Invalid)
            ?.insertTimeRange(range, ofTrack = audio, atTime = kCMTimeZero.readValue(), error = null)
    }

    // Everything below is in "upright" pixels: phones record sideways and store a rotation (preferredTransform).
    val (outW, outH) = mainVideo.uprightSize()
    val (smallW, smallH) = smallVideo.uprightSize()
    val window = pip.layout(outW.toInt(), outH.toInt()).parts[1].second
    val windowX = window.left * outW
    val windowY = window.top * outH
    val windowW = (window.right - window.left) * outW
    val windowH = (window.bottom - window.top) * outH
    // Aspect fill: scale the small camera to cover the window, then crop away the overflow (like FILL_CENTER).
    val scale = max(windowW / smallW, windowH / smallH)
    val overflowX = (smallW * scale - windowW) / 2
    val overflowY = (smallH * scale - windowH) / 2

    val smallLayer =
        AVMutableVideoCompositionLayerInstruction.videoCompositionLayerInstructionWithAssetTrack(smallTrack).apply {
            val upright = smallVideo.preferredTransform
            val placed =
                CGAffineTransformConcat(
                    CGAffineTransformConcat(upright, CGAffineTransformMakeScale(scale, scale)),
                    CGAffineTransformMakeTranslation(windowX - overflowX, windowY - overflowY),
                )
            setTransform(placed, atTime = kCMTimeZero.readValue())
            // The crop is in the track's own (sideways) coordinates, so map the visible upright part back.
            val visible = CGRectMake(overflowX / scale, overflowY / scale, windowW / scale, windowH / scale)
            setCropRectangle(CGRectApplyAffineTransform(visible, CGAffineTransformInvert(upright)), atTime = kCMTimeZero.readValue())
        }
    val mainLayer =
        AVMutableVideoCompositionLayerInstruction.videoCompositionLayerInstructionWithAssetTrack(mainTrack).apply {
            setTransform(mainVideo.preferredTransform, atTime = kCMTimeZero.readValue())
        }
    val instruction =
        AVMutableVideoCompositionInstruction().apply {
            setTimeRange(range)
            setLayerInstructions(listOf(smallLayer, mainLayer)) // first = on top
        }
    val videoComposition =
        AVMutableVideoComposition().apply {
            setRenderSize(CGSizeMake(outW, outH))
            setFrameDuration(CMTimeMake(value = 1, timescale = 30))
            setInstructions(listOf(instruction))
        }

    val export = AVAssetExportSession(asset = mix, presetName = AVAssetExportPresetHighestQuality)
    export.outputURL = output
    export.outputFileType = AVFileTypeQuickTimeMovie
    export.setVideoComposition(videoComposition)
    suspendCancellableCoroutine { continuation ->
        export.exportAsynchronouslyWithCompletionHandler {
            if (export.status == AVAssetExportSessionStatusCompleted) {
                continuation.resume(Unit)
            } else {
                continuation.resumeWithException(IllegalStateException(export.error?.localizedDescription ?: "Video export failed"))
            }
        }
        continuation.invokeOnCancellation { export.cancelExport() }
    }
}

private fun AVAssetTrack.uprightSize(): Pair<Double, Double> = CGSizeApplyAffineTransform(naturalSize, preferredTransform).size()

private fun CValue<CGSize>.size(): Pair<Double, Double> = useContents { abs(width) to abs(height) }
