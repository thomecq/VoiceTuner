package com.voicetuner.pitch

import com.voicetuner.audio.NoteFrequencies
import com.voicetuner.model.Note
import kotlin.math.log2

class YinPitchDetector(
    private val sampleRate: Int = 44100,
    private val threshold: Float = 0.15f
) : PitchDetector {

    override fun detect(audioBuffer: FloatArray): PitchResult? {
        val halfSize = audioBuffer.size / 2

        // Step 1: Difference function
        val diff = difference(audioBuffer, halfSize)

        // Step 2: Cumulative mean normalized difference
        val cmnd = cumulativeMeanNormalize(diff)

        // Step 3: Absolute threshold
        val tauEstimate = absoluteThreshold(cmnd) ?: return null

        // Step 4: Parabolic interpolation
        val refinedTau = parabolicInterpolation(cmnd, tauEstimate)

        // Step 5: Convert to frequency
        val frequency = sampleRate.toFloat() / refinedTau

        // Validate frequency range (roughly C2 to C7)
        if (frequency < 65f || frequency > 2100f) return null

        val confidence = 1f - cmnd[tauEstimate]
        val closestNote = NoteFrequencies.closestNote(frequency) ?: return null
        val centsOffset = NoteFrequencies.centsFromNote(frequency, closestNote)

        return PitchResult(
            frequency = frequency,
            confidence = confidence,
            closestNote = closestNote,
            centsOffset = centsOffset
        )
    }

    private fun difference(buffer: FloatArray, halfSize: Int): FloatArray {
        val diff = FloatArray(halfSize)
        for (tau in 0 until halfSize) {
            var sum = 0f
            for (j in 0 until halfSize) {
                val delta = buffer[j] - buffer[j + tau]
                sum += delta * delta
            }
            diff[tau] = sum
        }
        return diff
    }

    private fun cumulativeMeanNormalize(diff: FloatArray): FloatArray {
        val cmnd = FloatArray(diff.size)
        cmnd[0] = 1f
        var runningSum = 0f

        for (tau in 1 until diff.size) {
            runningSum += diff[tau]
            cmnd[tau] = if (runningSum != 0f) {
                diff[tau] * tau / runningSum
            } else {
                1f
            }
        }

        return cmnd
    }

    private fun absoluteThreshold(cmnd: FloatArray): Int? {
        // Start from tau=2 to avoid trivially small lags
        val minTau = (sampleRate / 2100f).toInt().coerceAtLeast(2) // max freq ~2100Hz
        val maxTau = (sampleRate / 65f).toInt().coerceAtMost(cmnd.size - 1) // min freq ~65Hz

        for (tau in minTau..maxTau) {
            if (cmnd[tau] < threshold) {
                // Find the local minimum after this point
                var minTauLocal = tau
                while (minTauLocal + 1 < cmnd.size && cmnd[minTauLocal + 1] < cmnd[minTauLocal]) {
                    minTauLocal++
                }
                return minTauLocal
            }
        }

        return null
    }

    private fun parabolicInterpolation(cmnd: FloatArray, tauEstimate: Int): Float {
        if (tauEstimate <= 0 || tauEstimate >= cmnd.size - 1) {
            return tauEstimate.toFloat()
        }

        val s0 = cmnd[tauEstimate - 1]
        val s1 = cmnd[tauEstimate]
        val s2 = cmnd[tauEstimate + 1]

        val adjustment = (s2 - s0) / (2f * (2f * s1 - s2 - s0))

        return if (adjustment.isFinite()) {
            tauEstimate + adjustment
        } else {
            tauEstimate.toFloat()
        }
    }
}
