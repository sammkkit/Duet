@file:OptIn(UnstableApi::class)

package com.samkit.duet.core.camera

import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.VideoCompositorSettings
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Crop
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.samkit.duet.core.model.LENS
import com.samkit.duet.core.model.SavedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Merges the two PiP recordings into one video laid out like the screen, using the shared [layout] math.
 * The small camera is centre-cropped to the window's shape and drawn over the main one; sound comes from the main.
 * ponytail: the window is a plain rectangle in the video (rounded/circle needs a custom GL mask effect).
 */
internal suspend fun Context.composeVideo(
    files: Map<LENS, File>,
    pip: PhotoComposition.Pip,
    name: String,
): SavedFile {
    val main = files.getValue(pip.main)
    val small = files.getValue(if (pip.main == LENS.BACK) LENS.FRONT else LENS.BACK)
    val (outW, outH) = uprightSize(main)
    val (smallW, smallH) = uprightSize(small)
    val window = pip.layout(outW, outH).parts[1].second
    val windowW = (window.right - window.left) * outW
    val windowH = (window.bottom - window.top) * outH

    // Crop works in NDC (-1..1): keep the centre part with the window's aspect ratio, like FILL_CENTER.
    val smallAspect = smallW.toFloat() / smallH
    val windowAspect = windowW / windowH
    val (crop, croppedW) =
        if (smallAspect > windowAspect) {
            val k = windowAspect / smallAspect
            Crop(-k, k, -1f, 1f) to smallW * k
        } else {
            val k = smallAspect / windowAspect
            Crop(-1f, 1f, -k, k) to smallW.toFloat()
        }
    val overlay =
        StaticOverlaySettings
            .Builder()
            .setScale(windowW / croppedW, windowW / croppedW)
            // Window centre on the background, in NDC (y points up).
            .setBackgroundFrameAnchor(window.left + window.right - 1f, 1f - (window.top + window.bottom))
            .build()

    val composition =
        Composition
            .Builder(
                // Input 0 is drawn on top: the small window first, the full-screen camera behind it.
                EditedMediaItemSequence
                    .Builder(
                        EditedMediaItem
                            .Builder(MediaItem.fromUri(Uri.fromFile(small)))
                            .setRemoveAudio(true)
                            .setEffects(Effects(emptyList(), listOf(crop)))
                            .build(),
                    ).build(),
                EditedMediaItemSequence.Builder(EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(main))).build()).build(),
            ).setVideoCompositorSettings(
                object : VideoCompositorSettings {
                    override fun getOutputSize(inputSizes: List<Size>): Size = inputSizes[1]

                    override fun getOverlaySettings(
                        inputId: Int,
                        presentationTimeUs: Long,
                    ) = if (inputId == 0) overlay else StaticOverlaySettings.Builder().build()
                },
            ).build()

    val out = File(cacheDir, "$name.mp4")
    try {
        // Transformer must be created and started on a thread with a Looper.
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val transformer =
                    Transformer
                        .Builder(this@composeVideo)
                        .addListener(
                            object : Transformer.Listener {
                                override fun onCompleted(
                                    composition: Composition,
                                    exportResult: ExportResult,
                                ) = continuation.resume(Unit)

                                override fun onError(
                                    composition: Composition,
                                    exportResult: ExportResult,
                                    exportException: ExportException,
                                ) = continuation.resumeWithException(exportException)
                            },
                        ).build()
                transformer.start(composition, out.absolutePath)
                continuation.invokeOnCancellation { transformer.cancel() }
            }
        }
        return saveVideoFileToGallery(out, name)
    } finally {
        out.delete()
    }
}

/** Width and height as the video plays (phones record sideways and store a rotation tag). */
private fun uprightSize(file: File): Pair<Int, Int> =
    MediaMetadataRetriever().run {
        try {
            setDataSource(file.absolutePath)
            val w = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)!!.toInt()
            val h = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)!!.toInt()
            val rotation = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toInt() ?: 0
            if (rotation % 180 == 0) w to h else h to w
        } finally {
            release()
        }
    }

/** Copies a finished video file into DCIM/Camera, hidden (IS_PENDING) until fully written. */
internal suspend fun Context.saveVideoFileToGallery(
    file: File,
    name: String,
): SavedFile =
    withContext(Dispatchers.IO) {
        val values =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.mp4")
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, GALLERY_FOLDER)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }
        val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: error("MediaStore insert failed")
        try {
            contentResolver.openOutputStream(uri)?.use { output -> file.inputStream().use { it.copyTo(output) } }
                ?: error("No output stream")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentResolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            }
        } catch (e: Exception) {
            contentResolver.delete(uri, null, null) // don't leave a broken half-file in the gallery
            throw e
        }
        SavedFile(uri.toString())
    }
