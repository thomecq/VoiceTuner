package com.voicetuner.audio

import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

class PianoSynthesizer {

    fun synthesizeNote(frequency: Float, durationMs: Long = AudioConfig.NOTE_DURATION_MS): ShortArray {
        val totalSamples = (AudioConfig.SAMPLE_RATE * durationMs / 1000).toInt()
        val samples = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val signal = generateHarmonicSeries(frequency, i, totalSamples)
            val envelope = applyEnvelope(i, totalSamples)
            val value = (signal * envelope * Short.MAX_VALUE * 0.8f).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            samples[i] = value.toShort()
        }

        return samples
    }

    fun synthesizeChord(frequencies: List<Float>, durationMs: Long = AudioConfig.NOTE_DURATION_MS): ShortArray {
        if (frequencies.isEmpty()) return ShortArray(0)
        if (frequencies.size == 1) return synthesizeNote(frequencies[0], durationMs)

        val totalSamples = (AudioConfig.SAMPLE_RATE * durationMs / 1000).toInt()
        val samples = ShortArray(totalSamples)
        val normFactor = 1.0f / frequencies.size

        for (i in 0 until totalSamples) {
            var mixedSignal = 0f
            for (freq in frequencies) {
                mixedSignal += generateHarmonicSeries(freq, i, totalSamples)
            }
            mixedSignal *= normFactor

            val envelope = applyEnvelope(i, totalSamples)
            val value = (mixedSignal * envelope * Short.MAX_VALUE * 0.8f).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            samples[i] = value.toShort()
        }

        return samples
    }

    private fun generateHarmonicSeries(frequency: Float, sampleIndex: Int, totalSamples: Int): Float {
        var signal = 0f
        val t = sampleIndex.toFloat() / AudioConfig.SAMPLE_RATE

        for (n in 1..AudioConfig.NUM_HARMONICS) {
            val harmonicFreq = frequency * n
            // Skip harmonics above Nyquist frequency
            if (harmonicFreq >= AudioConfig.SAMPLE_RATE / 2) break

            val amplitude = 1.0f / n.toFloat().pow(1.5f)
            signal += amplitude * sin(2.0 * PI * harmonicFreq * t).toFloat()
        }

        return signal
    }

    private fun applyEnvelope(sampleIndex: Int, totalSamples: Int): Float {
        val t = sampleIndex.toFloat() / AudioConfig.SAMPLE_RATE
        val totalDuration = totalSamples.toFloat() / AudioConfig.SAMPLE_RATE
        val releaseStart = totalDuration - AudioConfig.RELEASE_TIME

        return when {
            // Attack phase
            t < AudioConfig.ATTACK_TIME -> t / AudioConfig.ATTACK_TIME

            // Decay phase
            t < AudioConfig.ATTACK_TIME + AudioConfig.DECAY_TIME -> {
                val decayProgress = (t - AudioConfig.ATTACK_TIME) / AudioConfig.DECAY_TIME
                1.0f - (1.0f - AudioConfig.SUSTAIN_LEVEL) * decayProgress
            }

            // Release phase
            t > releaseStart -> {
                val releaseProgress = (t - releaseStart) / AudioConfig.RELEASE_TIME
                AudioConfig.SUSTAIN_LEVEL * (1.0f - releaseProgress).coerceAtLeast(0f)
            }

            // Sustain phase
            else -> AudioConfig.SUSTAIN_LEVEL
        }
    }
}
