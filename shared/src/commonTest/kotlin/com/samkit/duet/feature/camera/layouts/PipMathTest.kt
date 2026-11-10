package com.samkit.duet.feature.camera.layouts

import com.samkit.duet.core.model.PipCorner
import kotlin.test.Test
import kotlin.test.assertEquals

class PipMathTest {
    @Test
    fun nearestCornerIsTheQuadrantOfTheCenter() {
        assertEquals(PipCorner.TopStart, nearestPipCorner(10f, 10f, 400f, 600f))
        assertEquals(PipCorner.TopEnd, nearestPipCorner(390f, 10f, 400f, 600f))
        assertEquals(PipCorner.BottomStart, nearestPipCorner(10f, 590f, 400f, 600f))
        assertEquals(PipCorner.BottomEnd, nearestPipCorner(390f, 590f, 400f, 600f))
    }

    @Test
    fun cornerPositionRespectsMargin() {
        // 400×600 area, 100×150 window, 16 margin
        assertEquals(16f to 16f, pipCornerPosition(PipCorner.TopStart, 400f, 600f, 100f, 150f, 16f))
        assertEquals(284f to 434f, pipCornerPosition(PipCorner.BottomEnd, 400f, 600f, 100f, 150f, 16f))
    }

    @Test
    fun snappedWindowMapsBackToItsOwnCorner() {
        PipCorner.entries.forEach { corner ->
            val (x, y) = pipCornerPosition(corner, 400f, 600f, 100f, 150f, 16f)
            assertEquals(corner, nearestPipCorner(x + 50f, y + 75f, 400f, 600f))
        }
    }
}
