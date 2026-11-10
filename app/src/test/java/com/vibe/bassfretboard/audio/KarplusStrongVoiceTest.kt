package com.vibe.bassfretboard.audio

import java.util.Random
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KarplusStrongVoiceTest {
    private fun render(freq: Double, seconds: Double, sampleRate: Double = 44100.0): DoubleArray {
        val voice = KarplusStrongVoice(sampleRate, Random(42))
        voice.frequency = freq
        voice.blend = 0.0
        voice.stretch = 0.85
        voice.pluck()
        val n = (seconds * sampleRate).toInt()
        return DoubleArray(n) { voice.next() }
    }

    private fun rms(x: DoubleArray, from: Int, to: Int): Double {
        var sum = 0.0
        for (i in from until to) sum += x[i] * x[i]
        return kotlin.math.sqrt(sum / (to - from))
    }

    @Test
    fun outputStaysFiniteAndBounded() {
        val out = render(110.0, 1.0)
        for (v in out) {
            assertTrue("value $v not finite", !v.isNaN() && !v.isInfinite())
            assertTrue("value $v out of range", abs(v) <= 1.0 + 1e-9)
        }
    }

    @Test
    fun pluckDecaysOverTime() {
        val out = render(110.0, 2.0)
        val sr = 44100
        val early = rms(out, sr / 10, sr / 5)
        val late = rms(out, 3 * sr / 2, 2 * sr - 1)
        assertTrue("expected decay: early=$early late=$late", early > late * 1.5)
    }

    @Test
    fun pitchIsApproximatelyCorrect() {
        val freq = 110.0
        val sr = 44100.0
        val out = render(freq, 1.0, sr)
        val measured = estimateFrequency(out, sr)
        assertEquals(freq, measured, freq * 0.10)
    }

    private fun estimateFrequency(out: DoubleArray, sr: Double): Double {
        val start = (0.2 * sr).toInt()
        val len = (0.4 * sr).toInt()
        var mean = 0.0
        for (i in start until start + len) mean += out[i]
        mean /= len
        val x = DoubleArray(len) { out[start + it] - mean }
        var e0 = 0.0
        for (v in x) e0 += v * v
        var bestLag = 1
        var bestAbsR = 0.0
        for (lag in (sr / 400).toInt()..(sr / 40).toInt()) {
            var r = 0.0
            for (i in 0 until len - lag) r += x[i] * x[i + lag]
            r /= e0
            if (abs(r) > bestAbsR) {
                bestAbsR = abs(r)
                bestLag = lag
            }
        }
        return sr / bestLag
    }

    @Test
    fun lowBassNoteFitsBuffer() {
        val voice = KarplusStrongVoice(44100.0, Random(1))
        voice.frequency = 30.87
        voice.pluck()
        repeat(44100) { assertTrue(!voice.next().isNaN()) }
    }
}
