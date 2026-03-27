package com.voicetuner.model

enum class Accuracy {
    TOO_LOW,
    CORRECT,
    TOO_HIGH,
    NO_PITCH
}

data class PitchFeedback(
    val targetNote: Note,
    val detectedNote: Note?,
    val detectedFrequency: Float,
    val centsOffset: Float,
    val semitonesOffset: Float,
    val accuracy: Accuracy,
    val isInTune: Boolean
)
