package com.vibe.bassfretboard.model

import kotlin.math.abs

data class FretPosition(
    val stringIndex: Int,
    val fret: Int,
    val midi: Int,
)

class PointerNoteMapper(private val tuning: Tuning) {
    fun map(
        x: Float,
        y: Float,
        geometry: FretboardGeometry,
        stringCoords: List<Float>,
        stringHitRadius: Float,
    ): FretPosition? {
        if (stringCoords.isEmpty()) return null
        var best = -1
        var bestDist = Float.MAX_VALUE
        for (i in stringCoords.indices) {
            val d = abs(y - stringCoords[i])
            if (d < bestDist) {
                bestDist = d
                best = i
            }
        }
        if (best < 0 || bestDist > stringHitRadius) return null
        val fret = geometry.fretAtX(x)
        return FretPosition(best, fret, tuning.noteAt(best, fret))
    }
}
