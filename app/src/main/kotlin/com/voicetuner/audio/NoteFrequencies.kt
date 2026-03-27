package com.voicetuner.audio

import com.voicetuner.model.Note
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.roundToInt

object NoteFrequencies {
    // C3 (MIDI 48) through B5 (MIDI 83)
    const val MIN_MIDI = 48
    const val MAX_MIDI = 83

    private val notes: List<Note> by lazy {
        (MIN_MIDI..MAX_MIDI).map { Note.fromMidi(it) }
    }

    fun getAllNotes(): List<Note> = notes

    fun getNote(midiNumber: Int): Note? =
        notes.find { it.midiNumber == midiNumber }

    fun frequencyToMidi(frequency: Float): Int {
        return (69 + 12 * log2(frequency / 440f)).roundToInt()
    }

    fun closestNote(frequency: Float): Note? {
        val midi = frequencyToMidi(frequency)
        return getNote(midi.coerceIn(MIN_MIDI, MAX_MIDI))
    }

    fun centsFromNote(frequency: Float, note: Note): Float {
        return 1200f * log2(frequency / note.frequency).toFloat()
    }

    fun semitonesFromNote(frequency: Float, note: Note): Float {
        return 12f * log2(frequency / note.frequency).toFloat()
    }

    fun getWhiteKeys(): List<Note> = notes.filter { !it.isBlack }

    fun getBlackKeys(): List<Note> = notes.filter { it.isBlack }
}
