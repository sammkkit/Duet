package com.samkit.duet.core.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import com.samkit.duet.core.model.LENS
import com.samkit.duet.core.model.SavedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.roundToInt

// Concurrent cameras already stream small frames; this caps memory if a phone hands back full-size photos.
private const val MAX_EDGE = 2048

// Same folder as the phone's own camera app, so photos show up in the normal Camera album.
internal const val GALLERY_FOLDER = "DCIM/Camera"

/** Takes a photo into memory (not a file), upright, as a Bitmap. Decoding runs off the camera thread. */
internal suspend fun ImageCapture.captureBitmap(): Bitmap =
    suspendCancellableCoroutine { continuation ->
        takePicture(
            Dispatchers.Default.asExecutor(),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = image.use { it.toBitmap().rotated(it.imageInfo.rotationDegrees) }
                    continuation.resume(bitmap)
                }

                override fun onError(exception: ImageCaptureException) {
                    continuation.resumeWithException(exception)
                }
            },
        )
    }

/** Draws both cameras into one image using the shared layout math, so the photo matches the screen. */
internal fun composePhoto(
    shots: Map<LENS, Bitmap>,
    composition: PhotoComposition,
): Bitmap {
    val base =
        when (composition) {
            is PhotoComposition.Pip -> shots.getValue(composition.main)
            is PhotoComposition.Split -> shots.getValue(composition.first)
        }
    val scale = minOf(1f, MAX_EDGE.toFloat() / max(base.width, base.height))
    val width = (base.width * scale).roundToInt()
    val height = (base.height * scale).roundToInt()
    val layout = composition.layout(width, height)

    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    layout.parts.forEachIndexed { index, (lens, rect) ->
        val dst = RectF(rect.left * width, rect.top * height, rect.right * width, rect.bottom * height)
        val isWindow = composition is PhotoComposition.Pip && index == 1
        canvas.save()
        if (isWindow) canvas.clipPath(windowPath(dst, layout.circleWindow, width))
        canvas.drawBitmap(shots.getValue(lens), centerCrop(shots.getValue(lens), dst), dst, paint)
        canvas.restore()
    }
    return output
}

private fun windowPath(
    dst: RectF,
    circle: Boolean,
    imageWidth: Int,
): Path =
    Path().apply {
        if (circle) {
            addOval(dst, Path.Direction.CW)
        } else {
            val radius = 20f / 374f * imageWidth // same 20 dp corner as the on-screen window
            addRoundRect(dst, radius, radius, Path.Direction.CW)
        }
    }

/** The part of [bitmap] that fills [dst] without stretching, like the preview's FILL_CENTER. */
private fun centerCrop(
    bitmap: Bitmap,
    dst: RectF,
): Rect {
    val dstAspect = dst.width() / dst.height()
    val srcAspect = bitmap.width.toFloat() / bitmap.height
    return if (srcAspect > dstAspect) {
        val w = (bitmap.height * dstAspect).roundToInt()
        val left = (bitmap.width - w) / 2
        Rect(left, 0, left + w, bitmap.height)
    } else {
        val h = (bitmap.width / dstAspect).roundToInt()
        val top = (bitmap.height - h) / 2
        Rect(0, top, bitmap.width, top + h)
    }
}

private fun Bitmap.rotated(degrees: Int): Bitmap {
    if (degrees == 0) return this
    val rotated = Bitmap.createBitmap(this, 0, 0, width, height, Matrix().apply { postRotate(degrees.toFloat()) }, true)
    recycle()
    return rotated
}

/** Saves a JPEG to DCIM/Camera. IS_PENDING hides the half-written file from the gallery until it's complete. */
internal suspend fun Context.saveBitmapToGallery(
    bitmap: Bitmap,
    name: String,
): SavedFile =
    withContext(Dispatchers.IO) {
        val values =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, GALLERY_FOLDER)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("MediaStore insert failed")
        try {
            contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) } ?: error("No output stream")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentResolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            }
        } catch (e: Exception) {
            contentResolver.delete(uri, null, null) // don't leave a broken half-file in the gallery
            throw e
        }
        SavedFile(uri.toString())
    }
