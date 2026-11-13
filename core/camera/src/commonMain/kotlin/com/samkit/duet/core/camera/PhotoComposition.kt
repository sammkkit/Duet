package com.samkit.duet.core.camera

import com.samkit.duet.core.model.LENS
import com.samkit.duet.core.model.PipCorner
import com.samkit.duet.core.model.PipShape
import com.samkit.duet.core.model.SplitDirection

/** How to merge both cameras into one photo or video, mirroring the on-screen layout. */
sealed interface PhotoComposition {
    data class Pip(
        val main: LENS,
        val corner: PipCorner,
        val shape: PipShape,
    ) : PhotoComposition

    data class Split(
        val first: LENS,
        val direction: SplitDirection,
        val ratio: Float,
    ) : PhotoComposition
}

/** A rectangle in 0..1 coordinates of the output image. */
data class NormRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

/** Where each camera goes, in drawing order (first is drawn underneath). */
data class PhotoLayout(
    val parts: List<Pair<LENS, NormRect>>,
    val circleWindow: Boolean = false,
)

// Same proportions as the screen: a 116×156 dp window (132 dp circle) with 16 dp margins in a ~374 dp wide preview.
private const val PREVIEW_WIDTH_DP = 374f
private const val WINDOW_WIDTH = 116f / PREVIEW_WIDTH_DP
private const val WINDOW_ASPECT = 156f / 116f
private const val CIRCLE_WIDTH = 132f / PREVIEW_WIDTH_DP
private const val MARGIN = 16f / PREVIEW_WIDTH_DP

fun PhotoComposition.layout(
    width: Int,
    height: Int,
): PhotoLayout =
    when (this) {
        is PhotoComposition.Pip -> pipLayout(width.toFloat() / height)
        is PhotoComposition.Split -> splitLayout()
    }

private fun PhotoComposition.Pip.pipLayout(aspect: Float): PhotoLayout {
    val circle = shape == PipShape.CIRCLE
    val w = if (circle) CIRCLE_WIDTH else WINDOW_WIDTH
    // Heights are scaled by the image aspect so the window keeps its on-screen shape in non-square photos.
    val h = (if (circle) w else w * WINDOW_ASPECT) * aspect
    val marginY = MARGIN * aspect
    val left = if (corner == PipCorner.TopStart || corner == PipCorner.BottomStart) MARGIN else 1f - MARGIN - w
    val top = if (corner == PipCorner.TopStart || corner == PipCorner.TopEnd) marginY else 1f - marginY - h
    val small = if (main == LENS.BACK) LENS.FRONT else LENS.BACK
    return PhotoLayout(
        parts = listOf(main to NormRect(0f, 0f, 1f, 1f), small to NormRect(left, top, left + w, top + h)),
        circleWindow = circle,
    )
}

private fun PhotoComposition.Split.splitLayout(): PhotoLayout {
    val second = if (first == LENS.BACK) LENS.FRONT else LENS.BACK
    val (a, b) =
        when (direction) {
            SplitDirection.TopBottom -> NormRect(0f, 0f, 1f, ratio) to NormRect(0f, ratio, 1f, 1f)
            SplitDirection.LeftRight -> NormRect(0f, 0f, ratio, 1f) to NormRect(ratio, 0f, 1f, 1f)
        }
    return PhotoLayout(parts = listOf(first to a, second to b))
}
