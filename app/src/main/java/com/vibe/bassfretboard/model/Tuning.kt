package com.vibe.bassfretboard.model

data class Tuning(
    val id: String,
    val label: String,
    val openNotes: List<Int>,
) {
    val stringCount: Int get() = openNotes.size

    fun noteAt(stringIndex: Int, fret: Int): Int = openNotes[stringIndex] + fret

    companion object {
        val Standard4 = Tuning("standard4", "4-string Standard (E A D G)", listOf(43, 38, 33, 28))
        val DropD = Tuning("dropd", "4-string Drop D (D A D G)", listOf(43, 38, 33, 26))
        val DStandard = Tuning("dstandard", "4-string D Standard (D G C F)", listOf(53, 48, 43, 38))
        val Standard5 = Tuning("standard5", "5-string Standard (B E A D G)", listOf(43, 38, 33, 28, 23))
        val Piccolo = Tuning("piccolo", "4-string Piccolo (E A D G, +1 octave)", listOf(55, 50, 45, 40))

        val All = listOf(Standard4, DropD, DStandard, Standard5, Piccolo)

        fun byId(id: String): Tuning = All.firstOrNull { it.id == id } ?: Standard4
    }
}
