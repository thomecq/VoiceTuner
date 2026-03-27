package com.voicetuner.pitch

interface PitchDetector {
    fun detect(audioBuffer: FloatArray): PitchResult?
}
