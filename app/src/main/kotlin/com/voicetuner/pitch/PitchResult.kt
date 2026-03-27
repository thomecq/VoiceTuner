package com.voicetuner.pitch

import com.voicetuner.model.Note

data class PitchResult(
    val frequency: Float,
    val confidence: Float,
    val closestNote: Note,
    val centsOffset: Float,
    val timestamp: Long = System.currentTimeMillis()
)
