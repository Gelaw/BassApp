package com.vibe.bassfretboard.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FretboardGeometryTest {
    private val g = FretboardGeometry(nutX = 100f, scaleLength = 800f, fretCount = 24)

    @Test
    fun nutIsAtFretZero() {
        assertEquals(100f, g.fretX(0), 1e-4f)
    }

    @Test
    fun twelfthFretIsHalfScaleLength() {
        assertEquals(100f + 400f, g.fretX(12), 0.5f)
    }

    @Test
    fun fretPositionsIncreaseMonotonically() {
        for (f in 0 until 24) {
            assertTrue(g.fretX(f) < g.fretX(f + 1))
        }
    }

    @Test
    fun leftOfNutIsOpenString() {
        assertEquals(0, g.fretAtX(50f))
        assertEquals(0, g.fretAtX(99f))
    }

    @Test
    fun betweenNutAndFirstFretIsFretOne() {
        val mid = (g.fretX(0) + g.fretX(1)) / 2f
        assertEquals(1, g.fretAtX(mid))
    }

    @Test
    fun betweenFirstAndSecondFretIsFretTwo() {
        val mid = (g.fretX(1) + g.fretX(2)) / 2f
        assertEquals(2, g.fretAtX(mid))
    }

    @Test
    fun exactlyOnTwelfthFretWireIsFretThirteen() {
        assertEquals(13, g.fretAtX(g.fretX(12)))
    }

    @Test
    fun beyondLastFretIsClamped() {
        assertEquals(24, g.fretAtX(100000f))
    }

    @Test
    fun fretCenterIsInsideZone() {
        for (f in 1..24) {
            val c = g.fretCenterX(f)
            assertEquals(f, g.fretAtX(c))
        }
    }
}
