package com.voicetuner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicetuner.audio.AudioEngine
import com.voicetuner.audio.NoteFrequencies
import com.voicetuner.game.NoteGenerator
import com.voicetuner.model.Chord
import com.voicetuner.model.GameConfig
import com.voicetuner.model.GameDifficulty
import com.voicetuner.model.GamePhase
import com.voicetuner.model.GameRound
import com.voicetuner.model.GameState
import com.voicetuner.model.GameType
import com.voicetuner.model.Note
import com.voicetuner.model.NoteScore
import com.voicetuner.model.RoundResult
import com.voicetuner.model.scoreFromCents
import com.voicetuner.pitch.MicrophoneCapture
import com.voicetuner.pitch.PitchResult
import com.voicetuner.pitch.YinPitchDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

class GameViewModel : ViewModel() {
    private val audioEngine = AudioEngine()
    private val microphoneCapture = MicrophoneCapture()
    private val pitchDetector = YinPitchDetector()

    private var captureJob: Job? = null
    private var autoStopJob: Job? = null
    private var silenceCheckJob: Job? = null
    private var playJob: Job? = null

    private val recentResults = mutableListOf<PitchResult>()
    private val medianWindowSize = 5

    companion object {
        const val DELAY_BEFORE_RECORDING_MS = 1400L
        const val RECORDING_DURATION_MS = 6000L
        const val MIN_SINGING_DURATION_MS = 800L
        const val SILENCE_AFTER_SINGING_MS = 500L
    }

    // Game state
    private val _gameState = MutableStateFlow(
        GameState(
            config = GameConfig(GameType.SINGLE_NOTES, GameDifficulty.EASY),
            rounds = emptyList(),
            phase = GamePhase.SETUP
        )
    )
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    // Recording state
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isWaitingToRecord = MutableStateFlow(false)
    val isWaitingToRecord: StateFlow<Boolean> = _isWaitingToRecord.asStateFlow()

    private val _currentPitchResult = MutableStateFlow<PitchResult?>(null)
    val currentPitchResult: StateFlow<PitchResult?> = _currentPitchResult.asStateFlow()

    // Chord mode: which note in the chord we're currently singing
    private val _currentChordNoteIndex = MutableStateFlow(0)
    val currentChordNoteIndex: StateFlow<Int> = _currentChordNoteIndex.asStateFlow()

    // Track if replay was used
    private val _replayUsed = MutableStateFlow(false)
    val replayUsed: StateFlow<Boolean> = _replayUsed.asStateFlow()

    // Per-round accumulated results for chords
    private val chordDetectedNotes = mutableListOf<Note?>()
    private val chordCentsOffsets = mutableListOf<Float>()
    private val chordNoteScores = mutableListOf<NoteScore>()

    // Singing tracking
    private var singingStartTime = 0L
    private var lastPitchDetectedTime = 0L
    private var hasSungEnough = false
    private var lastDetectedNote: Note? = null
    private var lastCentsOffset = 0f
    private val allSessionResults = mutableListOf<PitchResult>()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            val notes = (60..83).map { Note.fromMidi(it) }
            audioEngine.precomputeNotes(notes)
        }
    }

    fun startGame(config: GameConfig) {
        cancelAll()
        val rounds = NoteGenerator.generateRounds(config)
        _gameState.value = GameState(
            config = config,
            rounds = rounds,
            phase = GamePhase.PLAYING_NOTE
        )
        _replayUsed.value = false
        chordDetectedNotes.clear()
        chordCentsOffsets.clear()
        chordNoteScores.clear()
        playCurrentNote()
    }

    fun playCurrentNote() {
        val state = _gameState.value
        val round = state.rounds.getOrNull(state.currentRoundIndex) ?: return

        _gameState.update { it.copy(phase = GamePhase.PLAYING_NOTE) }

        playJob = viewModelScope.launch {
            if (round.isChord) {
                audioEngine.playChord(Chord(round.targetNotes))
            } else {
                audioEngine.playNote(round.targetNotes[0])
            }

            // Wait for note to finish + decay
            delay(DELAY_BEFORE_RECORDING_MS)

            if (round.isChord) {
                _gameState.update { it.copy(phase = GamePhase.COUNT_GUESS) }
            } else {
                startSinging()
            }
        }
    }

    fun replayNote() {
        if (_replayUsed.value) return
        _replayUsed.value = true

        val state = _gameState.value
        val round = state.rounds.getOrNull(state.currentRoundIndex) ?: return

        playJob?.cancel()
        cancelCapture()

        playJob = viewModelScope.launch {
            if (round.isChord) {
                audioEngine.playChord(Chord(round.targetNotes))
            } else {
                audioEngine.playNote(round.targetNotes[0])
            }
            delay(DELAY_BEFORE_RECORDING_MS)
            if (round.isChord) {
                _gameState.update { it.copy(phase = GamePhase.COUNT_GUESS) }
            } else {
                startSinging()
            }
        }
    }

    private var countGuessValue: Int? = null

    fun submitCountGuess(count: Int) {
        countGuessValue = count
        _currentChordNoteIndex.value = 0
        chordDetectedNotes.clear()
        chordCentsOffsets.clear()
        chordNoteScores.clear()
        startSinging()
    }

    private fun startSinging() {
        _gameState.update { it.copy(phase = GamePhase.SINGING) }
        _isWaitingToRecord.value = false
        startCapture()
    }

    private fun startCapture() {
        _isRecording.value = true
        _currentPitchResult.value = null
        recentResults.clear()
        allSessionResults.clear()
        singingStartTime = 0L
        lastPitchDetectedTime = 0L
        hasSungEnough = false
        lastDetectedNote = null
        lastCentsOffset = 0f

        autoStopJob = viewModelScope.launch {
            delay(RECORDING_DURATION_MS)
            onSingingFinished()
        }

        val state = _gameState.value
        val showLivePitch = state.config.difficulty == GameDifficulty.EASY

        captureJob = viewModelScope.launch {
            microphoneCapture.startCapture().collect { buffer ->
                val now = System.currentTimeMillis()
                val result = pitchDetector.detect(buffer)

                if (result != null && result.confidence > 0.5f) {
                    if (singingStartTime == 0L) singingStartTime = now
                    lastPitchDetectedTime = now
                    if (now - singingStartTime >= MIN_SINGING_DURATION_MS) hasSungEnough = true

                    silenceCheckJob?.cancel()

                    allSessionResults.add(result)
                    recentResults.add(result)
                    if (recentResults.size > medianWindowSize) recentResults.removeAt(0)

                    val smoothed = medianSmooth(recentResults)
                    if (smoothed != null) {
                        lastDetectedNote = smoothed.closestNote
                        // Compute cents against current target
                        val targetNote = getCurrentTargetNote()
                        if (targetNote != null) {
                            lastCentsOffset = NoteFrequencies.centsFromNote(smoothed.frequency, targetNote)
                        }
                        if (showLivePitch) {
                            _currentPitchResult.value = smoothed
                        }
                    }
                } else if (hasSungEnough && lastPitchDetectedTime > 0L) {
                    if (silenceCheckJob?.isActive != true) {
                        silenceCheckJob = viewModelScope.launch {
                            delay(SILENCE_AFTER_SINGING_MS)
                            onSingingFinished()
                        }
                    }
                }
            }
        }
    }

    private fun getCurrentTargetNote(): Note? {
        val state = _gameState.value
        val round = state.rounds.getOrNull(state.currentRoundIndex) ?: return null
        return if (round.isChord) {
            round.targetNotes.getOrNull(_currentChordNoteIndex.value)
        } else {
            round.targetNotes[0]
        }
    }

    private fun onSingingFinished() {
        cancelCapture()
        _isRecording.value = false
        _currentPitchResult.value = null

        // Use stable result from entire session
        val stableResult = stableSessionResult(allSessionResults)
        if (stableResult != null) {
            lastDetectedNote = stableResult.closestNote
        }

        val state = _gameState.value
        val round = state.rounds.getOrNull(state.currentRoundIndex) ?: return

        if (round.isChord) {
            // Save result for this chord note
            val targetNote = round.targetNotes.getOrNull(_currentChordNoteIndex.value)
            val cents = if (targetNote != null && lastDetectedNote != null) {
                NoteFrequencies.centsFromNote(lastDetectedNote!!.frequency, targetNote)
            } else {
                100f // miss
            }
            chordDetectedNotes.add(lastDetectedNote)
            chordCentsOffsets.add(cents)
            chordNoteScores.add(scoreFromCents(cents))

            val nextIndex = _currentChordNoteIndex.value + 1
            if (nextIndex < round.targetNotes.size) {
                // More notes to sing
                _currentChordNoteIndex.value = nextIndex
                viewModelScope.launch {
                    delay(300) // brief pause between notes
                    startSinging()
                }
                return
            }
            // All chord notes done — show result
            finishRound()
        } else {
            finishRound()
        }
    }

    private fun finishRound() {
        val state = _gameState.value
        val round = state.rounds.getOrNull(state.currentRoundIndex) ?: return

        val detectedNotes: List<Note?>
        val centsOffsets: List<Float>
        val noteScores: List<NoteScore>

        if (round.isChord) {
            detectedNotes = chordDetectedNotes.toList()
            centsOffsets = chordCentsOffsets.toList()
            noteScores = chordNoteScores.toList()
        } else {
            val targetNote = round.targetNotes[0]
            val cents = if (lastDetectedNote != null) {
                NoteFrequencies.centsFromNote(lastDetectedNote!!.frequency, targetNote)
            } else {
                100f
            }
            detectedNotes = listOf(lastDetectedNote)
            centsOffsets = listOf(cents)
            noteScores = listOf(scoreFromCents(cents))
        }

        // Scoring
        val notePoints = if (round.isChord) {
            noteScores.fold(0) { acc, score -> acc + if (score.points >= 20) 10 else if (score.points >= 10) 5 else 0 }
        } else {
            noteScores[0].points
        }

        val countPoints = if (round.isChord && countGuessValue != null) {
            if (countGuessValue == round.targetNotes.size) 10 else 0
        } else 0

        val isGoodRound = if (round.isChord) {
            noteScores.all { it.points >= 10 }
        } else {
            noteScores[0].points >= 20
        }

        val newStreak = if (isGoodRound) state.currentStreak + 1 else 0
        val streakBonus = if (isGoodRound && newStreak >= 3) 5 else 0
        val roundTotal = notePoints + countPoints + streakBonus

        val result = RoundResult(
            round = round,
            detectedNotes = detectedNotes,
            centsOffsets = centsOffsets,
            noteScores = noteScores,
            countGuess = countGuessValue,
            countCorrect = if (round.isChord) countGuessValue == round.targetNotes.size else null,
            notePoints = notePoints,
            countPoints = countPoints,
            streakBonus = streakBonus,
            totalScore = roundTotal
        )

        _gameState.update {
            it.copy(
                results = it.results + result,
                totalScore = it.totalScore + roundTotal,
                currentStreak = newStreak,
                bestStreak = maxOf(it.bestStreak, newStreak),
                phase = GamePhase.ROUND_RESULT
            )
        }

        countGuessValue = null
    }

    fun nextRound() {
        val state = _gameState.value
        val nextIndex = state.currentRoundIndex + 1

        if (nextIndex >= state.rounds.size) {
            _gameState.update { it.copy(phase = GamePhase.GAME_OVER) }
            return
        }

        _gameState.update {
            it.copy(
                currentRoundIndex = nextIndex,
                phase = GamePhase.PLAYING_NOTE
            )
        }
        _replayUsed.value = false
        chordDetectedNotes.clear()
        chordCentsOffsets.clear()
        chordNoteScores.clear()
        playCurrentNote()
    }

    fun resetGame() {
        cancelAll()
        _gameState.update {
            it.copy(
                results = emptyList(),
                currentRoundIndex = 0,
                totalScore = 0,
                currentStreak = 0,
                bestStreak = 0,
                phase = GamePhase.SETUP
            )
        }
    }

    private fun cancelCapture() {
        captureJob?.cancel()
        captureJob = null
        autoStopJob?.cancel()
        autoStopJob = null
        silenceCheckJob?.cancel()
        silenceCheckJob = null
        _isRecording.value = false
        _isWaitingToRecord.value = false
        microphoneCapture.stopCapture()
    }

    private fun cancelAll() {
        playJob?.cancel()
        playJob = null
        cancelCapture()
    }

    private fun stableSessionResult(results: List<PitchResult>): PitchResult? {
        if (results.isEmpty()) return null
        if (results.size < 4) return medianSmooth(results)

        val sorted = results.sortedBy { it.frequency }
        val trimCount = (sorted.size * 0.2f).toInt().coerceAtLeast(1)
        val trimmed = sorted.subList(trimCount, sorted.size - trimCount)
        if (trimmed.isEmpty()) return medianSmooth(results)

        return trimmed[trimmed.size / 2]
    }

    private fun medianSmooth(results: List<PitchResult>): PitchResult? {
        if (results.isEmpty()) return null
        if (results.size < 3) return results.last()
        val sorted = results.sortedBy { it.frequency }
        return sorted[sorted.size / 2]
    }

    override fun onCleared() {
        super.onCleared()
        cancelAll()
        audioEngine.release()
    }
}
