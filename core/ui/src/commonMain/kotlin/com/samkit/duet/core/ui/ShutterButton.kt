package com.samkit.duet.core.ui

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samkit.duet.core.designsystem.theme.DuetTheme
import com.samkit.duet.core.model.CaptureType

private enum class ShutterState { Photo, VideoIdle, Recording }

/**
 * Main capture button. Photo: white dot. Video: red dot. Recording: red rounded square.
 * One transition drives size, corner and color so they always move together.
 */
@Composable
fun ShutterButton(
    captureType: CaptureType,
    isRecording: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state =
        when {
            isRecording -> ShutterState.Recording
            captureType == CaptureType.PHOTO -> ShutterState.Photo
            else -> ShutterState.VideoIdle
        }
    val colors = MaterialTheme.colorScheme
    val transition = updateTransition(state, label = "shutter")
    val innerSize by transition.animateDp(label = "size") { if (it == ShutterState.Recording) 30.dp else 62.dp }
    val corner by transition.animateDp(label = "corner") { if (it == ShutterState.Recording) 8.dp else 31.dp }
    val innerColor by transition.animateColor(label = "color") { if (it == ShutterState.Photo) colors.onSurface else colors.error }

    val description =
        when (state) {
            ShutterState.Photo -> "Take photo"
            ShutterState.VideoIdle -> "Start recording"
            ShutterState.Recording -> "Stop recording"
        }

    Box(
        modifier =
            modifier
                .size(80.dp)
                .clip(CircleShape)
                .border(4.dp, colors.onSurface, CircleShape)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(innerSize).background(innerColor, RoundedCornerShape(corner)))
    }
}

@Preview
@Composable
private fun ShutterButtonPreview() {
    DuetTheme {
        Row(
            modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ShutterButton(captureType = CaptureType.PHOTO, isRecording = false, onClick = {})
            ShutterButton(captureType = CaptureType.VIDEO, isRecording = false, onClick = {})
            ShutterButton(captureType = CaptureType.VIDEO, isRecording = true, onClick = {})
        }
    }
}
