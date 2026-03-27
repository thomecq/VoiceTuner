package com.voicetuner.model

data class AttemptRecord(
    val timestamp: Long,
    val targetNote: Note,
    val detectedNote: Note?,
    val centsOffset: Float,
    val accuracy: Accuracy,
    val isInTune: Boolean
)
