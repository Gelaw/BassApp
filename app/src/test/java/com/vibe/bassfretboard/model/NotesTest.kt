package com.vibe.bassfretboard.model

import org.junit.Assert.assertEquals
import org.junit.Test

class NotesTest {
    @Test
    fun a4Is440Hz() {
        assertEquals(440.0, Notes.frequencyOf(69), 1e-9)
    }

    @Test
    fun lowE1IsAbout41Hz() {
        assertEquals(41.203, Notes.frequencyOf(28), 0.001)
    }

    @Test
    fun octaveDoublesFrequency() {
        assertEquals(Notes.frequencyOf(40) * 2.0, Notes.frequencyOf(52), 1e-9)
    }

    @Test
    fun noteNames() {
        assertEquals("E1", Notes.nameOf(28))
        assertEquals("A1", Notes.nameOf(33))
        assertEquals("G2", Notes.nameOf(43))
        assertEquals("C4", Notes.nameOf(60))
        assertEquals("A4", Notes.nameOf(69))
    }
}
