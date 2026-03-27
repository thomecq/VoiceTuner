package com.voicetuner.model

enum class GamePhase {
    SETUP,
    PLAYING_NOTE,
    COUNT_GUESS,
    SINGING,
    ROUND_RESULT,
    GAME_OVER
}

data class RoundResult(
    val round: GameRound,
    val detectedNotes: List<Note?>,
    val centsOffsets: List<Float>,
    val noteScores: List<NoteScore>,
    val countGuess: Int? = null,
    val countCorrect: Boolean? = null,
    val notePoints: Int,
    val countPoints: Int,
    val streakBonus: Int,
    val totalScore: Int
)

data class GameState(
    val config: GameConfig,
    val rounds: List<GameRound>,
    val results: List<RoundResult> = emptyList(),
    val currentRoundIndex: Int = 0,
    val totalScore: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val phase: GamePhase = GamePhase.SETUP
)
