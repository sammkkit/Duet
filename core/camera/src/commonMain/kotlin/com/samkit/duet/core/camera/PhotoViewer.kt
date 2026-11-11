package com.samkit.duet.core.camera

import androidx.compose.runtime.Composable
import com.samkit.duet.core.model.SavedFile

/** Opens a saved photo in the phone's own photo viewer, like tapping the thumbnail in the system camera. */
@Composable
expect fun rememberPhotoViewer(): (SavedFile) -> Unit
