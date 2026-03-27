package com.voicetuner.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import kotlin.math.abs

class NoteFrequenciesTest {

    @Test
    fun `A4 frequency is 440 Hz`() {
        val note = NoteFrequencies.getNote(69) // A4 = MIDI 69
        assertNotNull(note)
        assertEquals(440f, note!!.frequency, 0.01f)
    }

    @Test
    fun `C4 frequency is approximately 261_63 Hz`() {
        val note = NoteFrequencies.getNote(60) // C4 = MIDI 60
        assertNotNull(note)
        assertEquals(261.63f, note!!.frequency, 0.1f)
    }

    @Test
    fun `all notes are in correct range`() {
        val notes = NoteFrequencies.getAllNotes()
        assertEquals(36, notes.size) // C3 (48) to B5 (83) = 36 notes

        // C3 should be first
        assertEquals("C", notes.first().name)
        assertEquals(3, notes.first().octave)

        // H5 (B5 in German/Polish notation) should be last
        assertEquals("H", notes.last().name)
        assertEquals(5, notes.last().octave)
    }

    @Test
    fun `frequencyToMidi converts correctly`() {
        assertEquals(69, NoteFrequencies.frequencyToMidi(440f))
        assertEquals(60, NoteFrequencies.frequencyToMidi(261.63f))
    }

    @Test
    fun `closestNote finds correct note`() {
        val note = NoteFrequencies.closestNote(442f) // slightly sharp A4
        assertNotNull(note)
        assertEquals("A", note!!.name)
        assertEquals(4, note.octave)
    }

    @Test
    fun `centsFromNote is zero for exact pitch`() {
        val note = NoteFrequencies.getNote(69)!!
        val cents = NoteFrequencies.centsFromNote(440f, note)
        assertEquals(0f, cents, 0.1f)
    }

    @Test
    fun `centsFromNote is approximately 100 for one semitone`() {
        val a4 = NoteFrequencies.getNote(69)!! // A4 = 440 Hz
        val bbFreq = NoteFrequencies.getNote(70)!!.frequency // A#4/Bb4
        val cents = NoteFrequencies.centsFromNote(bbFreq, a4)
        assertEquals(100f, cents, 1f)
    }

    @Test
    fun `white and black keys partition correctly`() {
        val whites = NoteFrequencies.getWhiteKeys()
        val blacks = NoteFrequencies.getBlackKeys()
        assertEquals(36, whites.size + blacks.size)
        assertEquals(21, whites.size) // 7 white keys per octave * 3
        assertEquals(15, blacks.size) // 5 black keys per octave * 3
    }
}
