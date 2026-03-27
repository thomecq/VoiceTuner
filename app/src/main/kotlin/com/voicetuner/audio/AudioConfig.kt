package com.voicetuner.audio

import android.media.AudioFormat

object AudioConfig {
    const val SAMPLE_RATE = 44100
    const val CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO
    const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    const val NUM_HARMONICS = 8
    const val NOTE_DURATION_MS = 2000L

    // ADSR envelope parameters (in seconds)
    const val ATTACK_TIME = 0.005f
    const val DECAY_TIME = 0.2f
    const val SUSTAIN_LEVEL = 0.6f
    const val RELEASE_TIME = 0.3f
}
