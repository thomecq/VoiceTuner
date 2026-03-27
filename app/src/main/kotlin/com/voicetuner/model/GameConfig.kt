package com.voicetuner.model

enum class GameType { SINGLE_NOTES, CHORDS }
enum class GameDifficulty { EASY, STANDARD }

enum class OctaveRange(val label: String, val minMidi: Int, val maxMidi: Int, val chordBaseMax: Int) {
    LOW("C3–H3", 48, 59, 52),
    MIDDLE("C4–H4", 60, 71, 64),
    HIGH("C5–H5", 72, 83, 76),
    LOW_MID("C3–H4", 48, 71, 64),
    MID_HIGH("C4–H5", 60, 83, 76)
}

data class GameConfig(
    val type: GameType,
    val difficulty: GameDifficulty,
    val octaveRange: OctaveRange = OctaveRange.MIDDLE,
    val roundCount: Int = 10
)
