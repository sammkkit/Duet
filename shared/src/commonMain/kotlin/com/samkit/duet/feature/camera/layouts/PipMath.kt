package com.samkit.duet.feature.camera.layouts

import com.samkit.duet.core.model.PipCorner

/** Top-left position (px) of a [windowW]×[windowH] window snapped to [corner] inside [areaW]×[areaH] with [margin]. */
internal fun pipCornerPosition(
    corner: PipCorner,
    areaW: Float,
    areaH: Float,
    windowW: Float,
    windowH: Float,
    margin: Float,
): Pair<Float, Float> {
    val left = margin
    val right = areaW - windowW - margin
    val top = margin
    val bottom = areaH - windowH - margin
    return when (corner) {
        PipCorner.TopStart -> left to top
        PipCorner.TopEnd -> right to top
        PipCorner.BottomStart -> left to bottom
        PipCorner.BottomEnd -> right to bottom
    }
}

/** The corner whose quadrant contains the window's center. */
internal fun nearestPipCorner(
    centerX: Float,
    centerY: Float,
    areaW: Float,
    areaH: Float,
): PipCorner {
    val start = centerX < areaW / 2
    val top = centerY < areaH / 2
    return when {
        top && start -> PipCorner.TopStart
        top -> PipCorner.TopEnd
        start -> PipCorner.BottomStart
        else -> PipCorner.BottomEnd
    }
}
