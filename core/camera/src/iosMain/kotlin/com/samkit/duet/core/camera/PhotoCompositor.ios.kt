@file:OptIn(ExperimentalForeignApi::class)

package com.samkit.duet.core.camera

import com.samkit.duet.core.model.LENS
import com.samkit.duet.core.model.SavedFile
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGContextRestoreGState
import platform.CoreGraphics.CGContextSaveGState
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Photos.PHAccessLevelAddOnly
import platform.Photos.PHAssetCreationRequest
import platform.Photos.PHAssetResourceCreationOptions
import platform.Photos.PHAssetResourceTypePhoto
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIBezierPath
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max

// Same cap as Android: two full-size photos plus the merged one get big fast.
private const val MAX_EDGE = 2048.0

/** Draws both cameras into one JPEG using the shared layout math, so the photo matches the screen. */
internal fun composePhoto(
    shots: Map<LENS, NSData>,
    composition: PhotoComposition,
): NSData {
    val images = shots.mapValues { (_, data) -> UIImage(data = data) }
    val base =
        when (composition) {
            is PhotoComposition.Pip -> images.getValue(composition.main)
            is PhotoComposition.Split -> images.getValue(composition.first)
        }
    // UIImage.size already accounts for the photo's EXIF orientation, so this is the upright size.
    val (baseWidth, baseHeight) = base.size.useContents { width to height }
    val scale = minOf(1.0, MAX_EDGE / max(baseWidth, baseHeight))
    val width = baseWidth * scale
    val height = baseHeight * scale
    val layout = composition.layout(width.toInt(), height.toInt())

    val format = UIGraphicsImageRendererFormat.defaultFormat().apply { this.scale = 1.0 } // pixels, not points
    val renderer = UIGraphicsImageRenderer(size = CGSizeMake(width, height), format = format)
    return renderer.JPEGDataWithCompressionQuality(0.95) { _ ->
        layout.parts.forEachIndexed { index, (lens, rect) ->
            val dst = CGRectMake(rect.left * width, rect.top * height, (rect.right - rect.left) * width, (rect.bottom - rect.top) * height)
            val isWindow = composition is PhotoComposition.Pip && index == 1
            val clip =
                when {
                    !isWindow -> UIBezierPath.bezierPathWithRect(dst)
                    layout.circleWindow -> UIBezierPath.bezierPathWithOvalInRect(dst)
                    else -> UIBezierPath.bezierPathWithRoundedRect(dst, cornerRadius = 20.0 / 374.0 * width) // same 20 dp corner
                }
            val context = UIGraphicsGetCurrentContext()
            CGContextSaveGState(context)
            clip.addClip()
            val image = images.getValue(lens)
            image.drawInRect(image.aspectFill(dst))
            CGContextRestoreGState(context)
        }
    } ?: error("JPEG encoding failed")
}

/** The rect to draw this image in so it covers [dst] without stretching (the clip cuts the overflow), like FILL_CENTER. */
private fun UIImage.aspectFill(dst: CValue<CGRect>) =
    size.useContents {
        val imageWidth = width
        val imageHeight = height
        dst.useContents {
            val s = max(size.width / imageWidth, size.height / imageHeight)
            val w = imageWidth * s
            val h = imageHeight * s
            CGRectMake(origin.x + (size.width - w) / 2, origin.y + (size.height - h) / 2, w, h)
        }
    }

/** Saves a JPEG to the Photos library. Add-only access: iOS asks once, and we never read the user's photos. */
internal suspend fun saveToPhotos(
    data: NSData,
    name: String,
): SavedFile {
    val status =
        suspendCancellableCoroutine { continuation ->
            PHPhotoLibrary.requestAuthorizationForAccessLevel(PHAccessLevelAddOnly) { continuation.resume(it) }
        }
    check(status == PHAuthorizationStatusAuthorized || status == PHAuthorizationStatusLimited) { "No Photos access" }
    val file =
        suspendCancellableCoroutine { continuation ->
            var id = ""
            PHPhotoLibrary.sharedPhotoLibrary().performChanges(
                {
                    val request = PHAssetCreationRequest.creationRequestForAsset()
                    val options = PHAssetResourceCreationOptions().apply { originalFilename = "$name.jpg" }
                    request.addResourceWithType(PHAssetResourceTypePhoto, data, options)
                    id = request.placeholderForCreatedAsset?.localIdentifier.orEmpty()
                },
            ) { success, error ->
                if (success) {
                    continuation.resume(SavedFile(id))
                } else {
                    continuation.resumeWithException(IllegalStateException(error?.localizedDescription ?: "Couldn't save to Photos"))
                }
            }
        }
    // Main thread only, like every other read and write of recentPhotos.
    withContext(Dispatchers.Main) { recentPhotos[file.id] = data }
    return file
}
