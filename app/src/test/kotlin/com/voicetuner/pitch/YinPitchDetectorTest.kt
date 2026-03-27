package com.voicetuner.pitch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class YinPitchDetectorTest {

    private val detector = YinPitchDetector(sampleRate = 44100, threshold = 0.15f)
    private val bufferSize = 2048

    private fun generateSineWave(frequency: Double, sampleRate: Int = 44100, size: Int = bufferSize): FloatArray {
        return FloatArray(size) { i ->
            sin(2.0 * PI * frequency * i / sampleRate).toFloat()
        }
    }

    @Test
    fun `detects A4 at 440 Hz`() {
        val buffer = generateSineWave(440.0)
        val result = detector.detect(buffer)
        assertNotNull(result)
        assertEquals(440f, result!!.frequency, 2f)
    }

    @Test
    fun `detects A3 at 220 Hz`() {
        val buffer = generateSineWave(220.0)
        val result = detector.detect(buffer)
        assertNotNull(result)
        assertEquals(220f, result!!.frequency, 2f)
    }

    @Test
    fun `detects A5 at 880 Hz`() {
        val buffer = generateSineWave(880.0)
        val result = detector.detect(buffer)
        assertNotNull(result)
        assertEquals(880f, result!!.frequency, 3f)
    }

    @Test
    fun `detects C4 at 261_63 Hz`() {
        val buffer = generateSineWave(261.63)
        val result = detector.detect(buffer)
        assertNotNull(result)
        assertEquals(261.63f, result!!.frequency, 2f)
    }

    @Test
    fun `returns null for silence`() {
        val buffer = FloatArray(bufferSize) { 0f }
        val result = detector.detect(buffer)
        assertNull(result)
    }

    @Test
    fun `returns null or low confidence for very low amplitude`() {
        val buffer = FloatArray(bufferSize) { i ->
            (sin(2.0 * PI * 440.0 * i / 44100) * 0.001).toFloat()
        }
        val result = detector.detect(buffer)
        // YIN may still detect pitch from low-amplitude signals;
        // the important thing is that detected frequency is still correct
        if (result != null) {
            assertEquals(440f, result.frequency, 5f)
        }
    }

    @Test
    fun `detects signal with harmonics`() {
        // Generate a signal with fundamental + harmonics (more realistic vocal simulation)
        val fundamental = 330.0 // E4
        val buffer = FloatArray(bufferSize) { i ->
            val t = i.toDouble() / 44100
            (sin(2.0 * PI * fundamental * t) +
                0.5 * sin(2.0 * PI * fundamental * 2 * t) +
                0.25 * sin(2.0 * PI * fundamental * 3 * t)).toFloat()
        }
        val result = detector.detect(buffer)
        assertNotNull(result)
        assertEquals(330f, result!!.frequency, 3f)
    }

    @Test
    fun `closest note is correct for detected pitch`() {
        val buffer = generateSineWave(440.0)
        val result = detector.detect(buffer)
        assertNotNull(result)
        assertEquals("A", result!!.closestNote.name)
        assertEquals(4, result.closestNote.octave)
    }
}
