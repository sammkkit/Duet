package com.samkit.duet.feature.camera.layouts

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.samkit.duet.core.designsystem.components.CircleIconButton
import com.samkit.duet.core.designsystem.components.CircleIconButtonDefaults
import com.samkit.duet.core.designsystem.icons.DuetIcons
import com.samkit.duet.core.model.LENS
import com.samkit.duet.core.model.SplitDirection
import com.samkit.duet.core.ui.CameraChip
import com.samkit.duet.core.ui.ZoomChips
import com.samkit.duet.feature.camera.CameraAction
import com.samkit.duet.feature.camera.CameraUiState
import com.samkit.duet.feature.camera.MAX_SPLIT_RATIO
import com.samkit.duet.feature.camera.MIN_SPLIT_RATIO
import duet.shared.generated.resources.Res
import duet.shared.generated.resources.drag_split
import duet.shared.generated.resources.swap_left_right
import duet.shared.generated.resources.swap_top_bottom
import org.jetbrains.compose.resources.stringResource

private val Outer = 28.dp
private val Inner = 8.dp

/** Two halves, main camera first. Drag the handle to resize (30–70%). */
@Composable
internal fun SplitLayout(
    state: CameraUiState,
    onAction: (CameraAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vertical = state.splitDirection == SplitDirection.TopBottom
    val first = state.mainLens
    val second = first.other()
    // While dragging, the ratio lives here and the ViewModel hears about it once, on release.
    var dragRatio by remember { mutableStateOf<Float?>(null) }
    // Drop the local value only once the ViewModel's ratio has caught up, so the halves don't jump back for a frame.
    LaunchedEffect(state.splitRatio) { dragRatio = null }
    val ratio = dragRatio ?: state.splitRatio

    BoxWithConstraints(modifier) {
        val totalPx = (if (vertical) constraints.maxHeight else constraints.maxWidth).toFloat()
        val dragState =
            rememberDraggableState { deltaPx ->
                dragRatio = ((dragRatio ?: state.splitRatio) + deltaPx / totalPx).coerceIn(MIN_SPLIT_RATIO, MAX_SPLIT_RATIO)
            }
        if (vertical) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Half(
                    first,
                    RoundedCornerShape(Outer, Outer, Inner, Inner),
                    Alignment.TopEnd,
                    state,
                    onAction,
                    Modifier.weight(ratio).fillMaxWidth(),
                )
                Half(
                    second,
                    RoundedCornerShape(Inner, Inner, Outer, Outer),
                    Alignment.BottomEnd,
                    state,
                    onAction,
                    Modifier.weight(1 - ratio).fillMaxWidth(),
                )
            }
        } else {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Half(
                    first,
                    RoundedCornerShape(Outer, Inner, Inner, Outer),
                    Alignment.TopEnd,
                    state,
                    onAction,
                    Modifier.weight(ratio).fillMaxHeight(),
                )
                Half(
                    second,
                    RoundedCornerShape(Inner, Outer, Outer, Inner),
                    Alignment.TopEnd,
                    state,
                    onAction,
                    Modifier.weight(1 - ratio).fillMaxHeight(),
                )
            }
        }

        val boundary = if (vertical) maxHeight * ratio else maxWidth * ratio
        SplitHandle(
            vertical = vertical,
            modifier =
                (
                    if (vertical) {
                        Modifier.align(Alignment.TopCenter).offset(y = boundary - 22.dp)
                    } else {
                        Modifier.align(Alignment.CenterStart).offset(x = boundary - 22.dp)
                    }
                ).draggable(
                    state = dragState,
                    orientation = if (vertical) Orientation.Vertical else Orientation.Horizontal,
                    onDragStopped = { dragRatio?.let { onAction(CameraAction.ChangeSplitRatio(it)) } },
                ),
        )
        CircleIconButton(
            onClick = { onAction(CameraAction.SwapCameras) },
            icon = DuetIcons.SwapVertical,
            contentDescription = stringResource(if (vertical) Res.string.swap_top_bottom else Res.string.swap_left_right),
            size = CircleIconButtonDefaults.SmallSize,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier =
                if (vertical) {
                    Modifier.align(Alignment.TopEnd).offset(x = (-12).dp, y = boundary - 22.dp)
                } else {
                    Modifier.align(Alignment.BottomStart).offset(x = boundary - 22.dp, y = (-12).dp).rotate(90f)
                },
        )
    }
}

@Composable
private fun Half(
    lens: LENS,
    shape: Shape,
    // Corner away from the boundary, where the handle and swap button sit.
    zoomAlignment: Alignment,
    state: CameraUiState,
    onAction: (CameraAction) -> Unit,
    modifier: Modifier,
) {
    PreviewSurface(lens = lens, modifier = modifier, shape = shape) {
        CameraChip(lens = lens, modifier = Modifier.align(Alignment.TopStart).padding(16.dp))
        ZoomChips(
            levels = listOf(state.zoom),
            selected = state.zoom,
            onSelect = { onAction(CameraAction.SelectZoom(it)) },
            lens = lens,
            modifier = Modifier.align(zoomAlignment).padding(12.dp),
        )
    }
}

@Composable
private fun SplitHandle(
    vertical: Boolean,
    modifier: Modifier,
) {
    // 72×44 touch area around a 56×20 pill (rotated for left/right).
    val description = stringResource(Res.string.drag_split)
    Box(
        modifier =
            modifier
                .size(if (vertical) 72.dp else 44.dp, if (vertical) 44.dp else 72.dp)
                .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(if (vertical) 56.dp else 20.dp, if (vertical) 20.dp else 56.dp)
                .background(MaterialTheme.colorScheme.onSurface, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = DuetIcons.DragHandle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier.size(16.dp).rotate(if (vertical) 0f else 90f),
            )
        }
    }
}
