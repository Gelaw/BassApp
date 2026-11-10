package com.vibe.bassfretboard.model

import kotlin.math.pow

object Notes {
    private val NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    fun nameOf(midi: Int): String = NAMES[midi % 12] + (midi / 12 - 1)

    fun frequencyOf(midi: Int): Double = 440.0 * 2.0.pow((midi - 69) / 12.0)
}
