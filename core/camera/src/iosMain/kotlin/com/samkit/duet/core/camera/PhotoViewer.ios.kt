package com.samkit.duet.core.camera

import androidx.compose.runtime.Composable
import com.samkit.duet.core.model.SavedFile
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

// ponytail: opens the Photos app (newest photos first), not this exact photo. Add-only access can't look a photo up;
// an in-app viewer from recentPhotos is the upgrade if that matters.
@Composable
actual fun rememberPhotoViewer(): (SavedFile) -> Unit =
    { _ ->
        NSURL.URLWithString("photos-redirect://")?.let { UIApplication.sharedApplication.openURL(it, emptyMap<Any?, Any>(), null) }
    }
