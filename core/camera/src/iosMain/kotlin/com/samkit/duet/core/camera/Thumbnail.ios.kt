@file:OptIn(ExperimentalForeignApi::class)

package com.samkit.duet.core.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.samkit.duet.core.model.SavedFile
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.posix.memcpy

/**
 * Add-only Photos access can save but never read back, so the last capture's bytes are kept here.
 * Cleared at every new capture: at most one shot's photos (merged + originals) stay in memory.
 */
internal val recentPhotos = mutableMapOf<String, NSData>()

@Composable
actual fun rememberThumbnail(file: SavedFile): ImageBitmap? =
    produceState<ImageBitmap?>(null, file) {
        val data = recentPhotos[file.id] ?: return@produceState
        value = withContext(Dispatchers.Default) { runCatching { data.thumbnail() }.getOrNull() }
    }.value

private fun NSData.thumbnail(): ImageBitmap {
    val image = UIImage(data = this)
    val (width, height) = image.size.useContents { width to height }
    val scale = THUMBNAIL_SIZE / maxOf(width, height)
    val format = UIGraphicsImageRendererFormat.defaultFormat().apply { this.scale = 1.0 }
    val renderer = UIGraphicsImageRenderer(size = CGSizeMake(width * scale, height * scale), format = format)
    // Re-encoding through UIKit also applies the EXIF rotation, which Skia would ignore.
    val jpeg =
        renderer.JPEGDataWithCompressionQuality(0.8) { _ ->
            image.drawInRect(CGRectMake(0.0, 0.0, width * scale, height * scale))
        } ?: error("Thumbnail encoding failed")
    return Image.makeFromEncoded(jpeg.toByteArray()).toComposeImageBitmap()
}

private fun NSData.toByteArray(): ByteArray =
    ByteArray(length.toInt()).apply {
        if (isNotEmpty()) usePinned { memcpy(it.addressOf(0), bytes, length) }
    }
