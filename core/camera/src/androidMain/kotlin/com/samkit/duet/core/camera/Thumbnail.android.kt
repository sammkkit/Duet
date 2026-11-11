package com.samkit.duet.core.camera

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.samkit.duet.core.model.SavedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
actual fun rememberThumbnail(file: SavedFile): ImageBitmap? {
    val context = LocalContext.current
    return produceState<ImageBitmap?>(null, file) {
        value = withContext(Dispatchers.IO) { runCatching { context.loadThumbnail(file) }.getOrNull() }
    }.value
}

private fun Context.loadThumbnail(file: SavedFile): ImageBitmap {
    val uri = Uri.parse(file.id)
    // loadThumbnail reads the photo's rotation; the fallback doesn't.
    // ponytail: Android 9 and lower may show the thumbnail sideways, add ExifInterface if anyone uses one.
    val bitmap =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentResolver.loadThumbnail(uri, Size(THUMBNAIL_SIZE, THUMBNAIL_SIZE), null)
        } else {
            val options = BitmapFactory.Options().apply { inSampleSize = 8 }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        }
    return checkNotNull(bitmap) { "Can't read $uri" }.asImageBitmap()
}
