package com.samkit.duet.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class FormattingTest {
    @Test
    fun elapsedIsPaddedMinutesAndSeconds() {
        assertEquals("00:00", formatElapsed(0.seconds))
        assertEquals("00:42", formatElapsed(42.seconds))
        assertEquals("12:05", formatElapsed(12.minutes + 5.seconds))
    }

    @Test
    fun zoomDropsLeadingZeroAndWholeDecimals() {
        assertEquals(".5", formatZoom(0.5f))
        assertEquals("1", formatZoom(1f))
        assertEquals("2", formatZoom(2f))
        assertEquals("1.5", formatZoom(1.5f))
    }
}
