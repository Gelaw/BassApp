package com.vibe.bassfretboard.model

import kotlin.math.pow

class FretboardGeometry(
    val nutX: Float,
    val scaleLength: Float,
    val fretCount: Int,
) {
    fun fretX(fret: Int): Float = nutX + scaleLength * (1f - 2f.pow(-fret / 12f))

    fun fretAtX(x: Float): Int {
        if (x < nutX) return 0
        var n = 0
        while (n < fretCount && x >= fretX(n + 1)) n++
        return (n + 1).coerceAtMost(fretCount)
    }

    fun fretCenterX(fret: Int): Float =
        if (fret == 0) nutX * 0.5f
        else (fretX(fret - 1) + fretX(fret)) / 2f

    companion object {
        const val MIN_FRET_COUNT = 12
        const val MAX_FRET_COUNT = 24
    }
}
