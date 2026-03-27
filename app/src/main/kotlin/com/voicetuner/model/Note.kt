package com.voicetuner.model

data class Note(
    val name: String,
    val octave: Int,
    val midiNumber: Int,
    val frequency: Float,
    val isBlack: Boolean = false
) {
    val displayName: String
        get() = "$name$octave"

    companion object {
        val NOTE_NAMES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "H")
        val BLACK_NOTES = setOf("C#", "D#", "F#", "G#", "A#")

        fun fromMidi(midi: Int): Note {
            val octave = (midi / 12) - 1
            val noteIndex = midi % 12
            val name = NOTE_NAMES[noteIndex]
            val frequency = 440f * Math.pow(2.0, (midi - 69.0) / 12.0).toFloat()
            return Note(
                name = name,
                octave = octave,
                midiNumber = midi,
                frequency = frequency,
                isBlack = name in BLACK_NOTES
            )
        }
    }
}
