package com.vibe.bassfretboard.audio

interface AudioEngine {
    val sampleRate: Int

    fun start()

    fun stop()

    fun noteOn(midi: Int, velocity: Float = 1f): Long

    fun noteOff(handle: Long)

    fun allNotesOff()
}
