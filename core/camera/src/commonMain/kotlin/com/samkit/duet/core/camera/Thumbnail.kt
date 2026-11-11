package com.samkit.duet.core.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import com.samkit.duet.core.model.SavedFile

/** A small preview of a photo this app saved. Null while loading or if it can't be read. */
@Composable
expect fun rememberThumbnail(file: SavedFile): ImageBitmap?

internal const val THUMBNAIL_SIZE = 256
