package com.samkit.duet.feature.camera.layouts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.samkit.duet.core.designsystem.components.CircleIconButton
import com.samkit.duet.core.designsystem.components.CircleIconButtonDefaults
import com.samkit.duet.core.designsystem.icons.DuetIcons
import com.samkit.duet.core.model.LENS
import com.samkit.duet.core.model.PipCorner
import com.samkit.duet.core.model.PipShape
import com.samkit.duet.core.ui.CameraChip
import com.samkit.duet.core.ui.ZoomChips
import com.samkit.duet.feature.camera.CameraAction
import com.samkit.duet.feature.camera.CameraUiState
import duet.shared.generated.resources.Res
import duet.shared.generated.resources.back_main
import duet.shared.generated.resources.front_main
import duet.shared.generated.resources.pip_window
import duet.shared.generated.resources.swap_cameras
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

private val PipZoomLevels = listOf(0.5f, 1f, 2f)
private val Margin = 16.dp

/**
 * Video call: main camera fills the area, the other sits in a small window.
 * Drag the window anywhere; on release it springs to the nearest corner.
 */
@Composable
internal fun PipLayout(
    state: CameraUiState,
    onAction: (CameraAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val main = state.mainLens
    val small = main.other()

    PreviewSurface(lens = main, modifier = modifier) {
        CameraChip(
            lens = main,
            label =
                stringResource(
                    if (main ==
                        LENS.BACK
                    ) {
                        Res.string.back_main
                    } else {
                        Res.string.front_main
                    },
                ),
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
        )
        ZoomChips(
            levels = PipZoomLevels,
            selected = state.zoom,
            onSelect = { onAction(CameraAction.SelectZoom(it)) },
            lens = main,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
        )

        BoxWithConstraints(Modifier.matchParentSize()) {
            val density = LocalDensity.current
            val circle = state.pipShape == PipShape.CIRCLE
            val windowW = if (circle) 132.dp else 116.dp
            val windowH = if (circle) 132.dp else 156.dp
            val px =
                with(
                    density,
                ) { PipPx(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat(), windowW.toPx(), windowH.toPx(), Margin.toPx()) }

            // Position lives here, not in the ViewModel: it changes every frame while dragging.
            val offset = remember { Animatable(px.target(state.pipCorner), Offset.VectorConverter) }
            val scope = rememberCoroutineScope()
            val windowDescription = stringResource(Res.string.pip_window)

            // Corner changed from outside (sheet, restore) or the area resized: move there.
            LaunchedEffect(state.pipCorner, px) { offset.animateTo(px.target(state.pipCorner), SnapSpring) }

            val atBottom = state.pipCorner == PipCorner.BottomStart || state.pipCorner == PipCorner.BottomEnd
            Box(
                Modifier
                    // Lambda offset is read in the layout phase: dragging moves the window without recomposing.
                    .offset { IntOffset(offset.value.x.roundToInt(), offset.value.y.roundToInt()) }
                    .pointerInput(px) {
                        detectDragGestures(
                            onDrag = { change, delta ->
                                change.consume()
                                scope.launch { offset.snapTo(offset.value + delta) }
                            },
                            onDragEnd = {
                                val corner = px.nearest(offset.value)
                                // Same corner: the LaunchedEffect won't fire, so animate back here.
                                scope.launch { offset.animateTo(px.target(corner), SnapSpring) }
                                onAction(CameraAction.ChangePipCorner(corner))
                            },
                        )
                    },
            ) {
                PreviewSurface(
                    lens = small,
                    modifier = Modifier.size(windowW, windowH).semantics { contentDescription = windowDescription },
                    shape = if (circle) CircleShape else MaterialTheme.shapes.large,
                    bordered = true,
                ) {
                    // The chip goes on the edge opposite the swap button so the two never overlap.
                    val chipAlignment =
                        when {
                            circle && atBottom -> Alignment.BottomCenter
                            circle -> Alignment.TopCenter
                            atBottom -> Alignment.BottomStart
                            else -> Alignment.TopStart
                        }
                    CameraChip(
                        lens = small,
                        small = true,
                        modifier = Modifier.align(chipAlignment).padding(if (circle) 12.dp else 8.dp),
                    )
                }
                // Half over the window's inner edge, so it never gets clipped by the preview.
                CircleIconButton(
                    onClick = { onAction(CameraAction.SwapCameras) },
                    icon = DuetIcons.Swap,
                    contentDescription = stringResource(Res.string.swap_cameras),
                    size = CircleIconButtonDefaults.SmallSize,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier =
                        Modifier
                            .align(if (atBottom) Alignment.TopCenter else Alignment.BottomCenter)
                            .offset(y = if (atBottom) (-22).dp else 22.dp),
                )
            }
        }
    }
}

private val SnapSpring = spring<Offset>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)

/** Pixel geometry of the PiP area. A data class so it can key effects and pointerInput. */
private data class PipPx(
    val areaW: Float,
    val areaH: Float,
    val windowW: Float,
    val windowH: Float,
    val margin: Float,
) {
    fun target(corner: PipCorner): Offset {
        val (x, y) = pipCornerPosition(corner, areaW, areaH, windowW, windowH, margin)
        return Offset(x, y)
    }

    fun nearest(topLeft: Offset): PipCorner = nearestPipCorner(topLeft.x + windowW / 2, topLeft.y + windowH / 2, areaW, areaH)
}
