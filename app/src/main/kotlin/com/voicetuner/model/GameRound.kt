package com.voicetuner.model

import kotlin.math.abs

data class GameRound(
    val index: Int,
    val targetNotes: List<Note>,
    val isChord: Boolean
)

enum class NoteScore(val points: Int, val label: String) {
    PERFECT(30, "Perfekcyjnie!"),
    GREAT(25, "Świetnie!"),
    GOOD(20, "Dobrze"),
    CLOSE(10, "Blisko"),
    MISS(0, "Pudło")
}

fun scoreFromCents(centsOffset: Float): NoteScore = when {
    abs(centsOffset) <= 5f -> NoteScore.PERFECT
    abs(centsOffset) <= 15f -> NoteScore.GREAT
    abs(centsOffset) <= 25f -> NoteScore.GOOD
    abs(centsOffset) <= 40f -> NoteScore.CLOSE
    else -> NoteScore.MISS
}
