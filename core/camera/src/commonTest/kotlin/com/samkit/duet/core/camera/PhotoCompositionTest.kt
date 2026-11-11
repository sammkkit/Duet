package com.samkit.duet.core.camera

import com.samkit.duet.core.model.LENS
import com.samkit.duet.core.model.PipCorner
import com.samkit.duet.core.model.PipShape
import com.samkit.duet.core.model.SplitDirection
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PhotoCompositionTest {
    private fun close(
        a: Float,
        b: Float,
    ) = assertTrue(abs(a - b) < 0.001f, "$a != $b")

    @Test
    fun pipMainFillsAndWindowSitsInItsCorner() {
        val layout = PhotoComposition.Pip(LENS.BACK, PipCorner.BottomEnd, PipShape.ROUNDED).layout(3000, 4000)
        val (mainLens, mainRect) = layout.parts[0]
        val (smallLens, window) = layout.parts[1]
        assertEquals(LENS.BACK, mainLens)
        assertEquals(NormRect(0f, 0f, 1f, 1f), mainRect)
        assertEquals(LENS.FRONT, smallLens)
        close(1f - 16f / 374f, window.right) // right margin
        assertTrue(window.bottom < 1f && window.top > 0.5f) // bottom half
    }

    @Test
    fun pipWindowKeepsItsOnScreenShapeInAPortraitPhoto() {
        val window =
            PhotoComposition
                .Pip(LENS.BACK, PipCorner.TopStart, PipShape.ROUNDED)
                .layout(3000, 4000)
                .parts[1]
                .second
        // In pixels: height / width should be 156 / 116, whatever the photo's aspect ratio.
        val ratio = ((window.bottom - window.top) * 4000) / ((window.right - window.left) * 3000)
        close(156f / 116f, ratio)
    }

    @Test
    fun circleWindowIsSquareInPixels() {
        val layout = PhotoComposition.Pip(LENS.FRONT, PipCorner.TopEnd, PipShape.CIRCLE).layout(3000, 4000)
        val window = layout.parts[1].second
        assertTrue(layout.circleWindow)
        close((window.bottom - window.top) * 4000, (window.right - window.left) * 3000)
    }

    @Test
    fun splitUsesRatioAndDirection() {
        val topBottom = PhotoComposition.Split(LENS.BACK, SplitDirection.TopBottom, 0.7f).layout(3000, 4000)
        assertEquals(LENS.BACK to NormRect(0f, 0f, 1f, 0.7f), topBottom.parts[0])
        assertEquals(LENS.FRONT to NormRect(0f, 0.7f, 1f, 1f), topBottom.parts[1])

        val leftRight = PhotoComposition.Split(LENS.FRONT, SplitDirection.LeftRight, 0.4f).layout(3000, 4000)
        assertEquals(LENS.FRONT to NormRect(0f, 0f, 0.4f, 1f), leftRight.parts[0])
        assertEquals(LENS.BACK to NormRect(0.4f, 0f, 1f, 1f), leftRight.parts[1])
    }
}
