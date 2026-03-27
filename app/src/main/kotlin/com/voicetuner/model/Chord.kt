package com.voicetuner.model

data class Chord(
    val notes: List<Note>
) {
    val displayName: String
        get() = notes.joinToString(" + ") { it.displayName }
}
