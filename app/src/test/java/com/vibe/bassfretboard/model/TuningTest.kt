package com.vibe.bassfretboard.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TuningTest {
    @Test
    fun standardBassTuningOpenNotes() {
        assertEquals(listOf(43, 38, 33, 28), Tuning.Standard4.openNotes)
    }

    @Test
    fun noteAtAddsFretToOpenString() {
        val t = Tuning.Standard4
        assertEquals(28, t.noteAt(3, 0))
        assertEquals(40, t.noteAt(3, 12))
        assertEquals(43, t.noteAt(0, 0))
        assertEquals(31, Tuning.Standard5.noteAt(4, 8))
    }

    @Test
    fun byIdFallsBackToStandard() {
        assertEquals(Tuning.Standard4, Tuning.byId("does-not-exist"))
        assertEquals(Tuning.DropD, Tuning.byId("dropd"))
    }

    @Test
    fun fiveStringHasFiveStrings() {
        assertEquals(5, Tuning.Standard5.stringCount)
    }
}
