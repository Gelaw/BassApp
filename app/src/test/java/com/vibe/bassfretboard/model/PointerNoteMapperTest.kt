package com.vibe.bassfretboard.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PointerNoteMapperTest {
    private val tuning = Tuning.Standard4
    private val mapper = PointerNoteMapper(tuning)
    private val geometry = FretboardGeometry(nutX = 100f, scaleLength = 800f, fretCount = 24)
    private val stringCoords = listOf(140f, 180f, 220f, 260f)

    @Test
    fun mapsToNearestStringAndFret() {
        val pos = mapper.map(x = (geometry.fretX(1) + geometry.fretX(2)) / 2f, y = 182f, geometry, stringCoords, 25f)
        assertEquals(1, pos?.stringIndex)
        assertEquals(2, pos?.fret)
        assertEquals(tuning.noteAt(1, 2), pos?.midi)
    }

    @Test
    fun openStringLeftOfNut() {
        val pos = mapper.map(x = 20f, y = 260f, geometry, stringCoords, 25f)
        assertEquals(3, pos?.stringIndex)
        assertEquals(0, pos?.fret)
        assertEquals(28, pos?.midi)
    }

    @Test
    fun tooFarFromAnyStringReturnsNull() {
        assertNull(mapper.map(x = 300f, y = 10f, geometry, stringCoords, 25f))
    }

    @Test
    fun fifthStringTuningMapsCorrectly() {
        val mapper5 = PointerNoteMapper(Tuning.Standard5)
        val coords5 = listOf(120f, 160f, 200f, 240f, 280f)
        val pos = mapper5.map(x = 50f, y = 280f, geometry, coords5, 25f)
        assertEquals(4, pos?.stringIndex)
        assertEquals(23, pos?.midi)
    }
}
