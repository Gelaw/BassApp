package com.vibe.bassfretboard.ui

import androidx.compose.ui.geometry.Offset
import com.vibe.bassfretboard.model.FretboardGeometry

class FretboardLayout(
    val width: Float,
    val height: Float,
    val vertical: Boolean,
    val mirrored: Boolean,
    fretCount: Int,
    stringCount: Int,
) {
    val nutU: Float
    val scaleLength: Float
    val stringCoords: List<Float>
    val geometry: FretboardGeometry
    val stringHitRadius: Float
    val bridgeU: Float

    init {
        if (vertical) {
            nutU = height * 0.06f
            scaleLength = height * 0.86f
            stringCoords = spaced(width * 0.18f, width * 0.82f, stringCount)
        } else {
            nutU = width * 0.07f
            scaleLength = width * 0.87f
            stringCoords = spaced(height * 0.14f, height * 0.86f, stringCount)
        }
        geometry = FretboardGeometry(nutU, scaleLength, fretCount)
        val acrossSpan = if (vertical) width * 0.64f else height * 0.72f
        stringHitRadius = acrossSpan / stringCount / 2f * 1.3f
        bridgeU = nutU + scaleLength + (if (vertical) height * 0.05f else width * 0.04f)
    }

    fun toLogical(p: Offset): Pair<Float, Float> =
        if (vertical) {
            val v = if (mirrored) width - p.x else p.x
            p.y to v
        } else {
            val u = if (mirrored) width - p.x else p.x
            u to p.y
        }

    fun toScreen(u: Float, v: Float): Offset =
        if (vertical) Offset(v, u)
        else Offset(if (mirrored) width - u else u, v)

    private fun spaced(from: Float, to: Float, count: Int): List<Float> =
        if (count <= 1) listOf((from + to) / 2f)
        else List(count) { i -> from + (to - from) * i / (count - 1) }
}
