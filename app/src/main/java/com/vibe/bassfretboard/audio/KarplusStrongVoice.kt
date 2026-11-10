/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * MODIFICATIONS (Apache License 2.0, section 4b): ported from Java to Kotlin;
 * the FrequencyProvider/SignalProvider/SynthesisTime plumbing was replaced by plain
 * constructor/setter parameters, and java.util.Random is injectable for
 * deterministic unit tests. The synthesis algorithm itself is unchanged.
 *
 * Original source:
 * https://github.com/raphlinus/music-synthesizer-for-android-old
 * (core/src/com/levien/synthesizer/core/model/oscillator/KarplusStrong.java)
 */
package com.vibe.bassfretboard.audio

import java.util.Random
import kotlin.math.roundToInt

class KarplusStrongVoice(
    private val sampleRate: Double,
    private val random: Random = Random(),
) {
    var frequency: Double = 110.0
    var blend: Double = 0.0
    var stretch: Double = 0.5
    var excitement: Double = 1.0

    private val buffer = DoubleArray(MAX_BUFFER_SIZE)
    private var bufferSize = 0
    private var current = 0
    private var previousOutput = 0.0
    private var trigger = false

    fun pluck() {
        trigger = true
    }

    fun next(): Double {
        if (current == 0) {
            bufferSize = (sampleRate / frequency).roundToInt().coerceIn(1, MAX_BUFFER_SIZE)
        }
        if (trigger) {
            trigger = false
            for (i in buffer.indices) {
                buffer[i] =
                    if (excitement == 0.0) 0.5
                    else excitement * (random.nextDouble() * 2.0 - 1.0)
            }
        }
        if (bufferSize == 0) return 0.0

        val output = buffer[current]
        val internalStretch = 1.0 - stretch
        val direction =
            if (blend == 0.0 || (blend != 1.0 && blend <= random.nextDouble())) -1.0 else 1.0
        val magnitude =
            if (internalStretch == 0.0 || (internalStretch != 1.0 && internalStretch <= random.nextDouble())) {
                buffer[current]
            } else {
                (buffer[current] + previousOutput) / 2.0
            }
        buffer[current] = direction * magnitude
        previousOutput = output
        current = (current + 1) % bufferSize
        return output
    }

    companion object {
        const val MAX_BUFFER_SIZE = 44100 / 20
    }
}
