package com.samkit.duet.core.camera

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.samkit.duet.core.model.SavedFile

@Composable
actual fun rememberPhotoViewer(): (SavedFile) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { file ->
            // ACTION_VIEW lets the user's gallery app (Google Photos, Samsung Gallery...) show it; the grant lets it read our URI.
            val intent =
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(Uri.parse(file.id), "image/jpeg")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            try {
                context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                // No gallery app installed: nothing to open it with.
            }
        }
    }
}
