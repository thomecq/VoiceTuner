package com.voicetuner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicetuner.audio.NoteFrequencies
import com.voicetuner.model.Accuracy
import com.voicetuner.model.AttemptRecord
import com.voicetuner.model.Note
import com.voicetuner.model.PitchFeedback
import com.voicetuner.pitch.MicrophoneCapture
import com.voicetuner.pitch.PitchResult
import com.voicetuner.pitch.YinPitchDetector
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

class PitchViewModel : ViewModel() {
    private val microphoneCapture = MicrophoneCapture()
    private val pitchDetector = YinPitchDetector()
    private var captureJob: Job? = null
    private var autoStopJob: Job? = null

    private val recentResults = mutableListOf<PitchResult>()
    private val medianWindowSize = 5

    companion object {
        const val DELAY_BEFORE_RECORDING_MS = 800L
        const val RECORDING_DURATION_MS = 4000L
    }

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isWaitingToRecord = MutableStateFlow(false)
    val isWaitingToRecord: StateFlow<Boolean> = _isWaitingToRecord.asStateFlow()

    private val _currentFeedback = MutableStateFlow<PitchFeedback?>(null)
    val currentFeedback: StateFlow<PitchFeedback?> = _currentFeedback.asStateFlow()

    private val _currentPitchResult = MutableStateFlow<PitchResult?>(null)
    val currentPitchResult: StateFlow<PitchResult?> = _currentPitchResult.asStateFlow()

    private val _attemptHistory = MutableStateFlow<List<AttemptRecord>>(emptyList())
    val attemptHistory: StateFlow<List<AttemptRecord>> = _attemptHistory.asStateFlow()

    private val _toleranceCents = MutableStateFlow(20f)
    val toleranceCents: StateFlow<Float> = _toleranceCents.asStateFlow()

    private var currentTargetNote: Note? = null

    private var stopPlaybackCallback: (() -> Unit)? = null

    fun onNotePlayed(targetNote: Note, onStopPlayback: (() -> Unit)? = null) {
        // Cancel any ongoing recording/waiting
        cancelAll()

        currentTargetNote = targetNote
        stopPlaybackCallback = onStopPlayback
        _currentFeedback.value = null
        _currentPitchResult.value = null
        recentResults.clear()

        // Wait for the piano sound to play, then stop it and start recording
        _isWaitingToRecord.value = true
        captureJob = viewModelScope.launch {
            delay(DELAY_BEFORE_RECORDING_MS)
            // Stop piano playback before starting microphone capture
            stopPlaybackCallback?.invoke()
            // Short extra pause for audio to fully stop
            delay(100L)
            _isWaitingToRecord.value = false
            startCapture(targetNote)
        }
    }

    private fun startCapture(targetNote: Note) {
        _isRecording.value = true

        // Auto-stop after duration
        autoStopJob = viewModelScope.launch {
            delay(RECORDING_DURATION_MS)
            finishRecording()
        }

        captureJob = viewModelScope.launch {
            microphoneCapture.startCapture().collect { buffer ->
                val result = pitchDetector.detect(buffer)
                if (result != null && result.confidence > 0.5f) {
                    recentResults.add(result)
                    if (recentResults.size > medianWindowSize) {
                        recentResults.removeAt(0)
                    }

                    val smoothedResult = medianSmooth(recentResults)
                    _currentPitchResult.value = smoothedResult

                    if (smoothedResult != null) {
                        _currentFeedback.value = createFeedback(targetNote, smoothedResult)
                    }
                }
            }
        }
    }

    private fun finishRecording() {
        captureJob?.cancel()
        captureJob = null
        autoStopJob?.cancel()
        autoStopJob = null
        _isRecording.value = false
        _isWaitingToRecord.value = false
        microphoneCapture.stopCapture()

        val feedback = _currentFeedback.value
        val target = currentTargetNote
        if (feedback != null && target != null) {
            val record = AttemptRecord(
                timestamp = System.currentTimeMillis(),
                targetNote = target,
                detectedNote = feedback.detectedNote,
                centsOffset = feedback.centsOffset,
                accuracy = feedback.accuracy,
                isInTune = feedback.isInTune
            )
            _attemptHistory.value = listOf(record) + _attemptHistory.value
        }
    }

    private fun cancelAll() {
        captureJob?.cancel()
        captureJob = null
        autoStopJob?.cancel()
        autoStopJob = null
        _isRecording.value = false
        _isWaitingToRecord.value = false
        microphoneCapture.stopCapture()
    }

    fun setTolerance(cents: Float) {
        _toleranceCents.value = cents.coerceIn(10f, 50f)
    }

    fun clearFeedback() {
        _currentFeedback.value = null
        _currentPitchResult.value = null
        recentResults.clear()
    }

    private fun createFeedback(targetNote: Note, result: PitchResult): PitchFeedback {
        val centsOffset = NoteFrequencies.centsFromNote(result.frequency, targetNote)
        val semitonesOffset = NoteFrequencies.semitonesFromNote(result.frequency, targetNote)
        val tolerance = _toleranceCents.value

        val accuracy = when {
            abs(centsOffset) <= tolerance -> Accuracy.CORRECT
            centsOffset < -tolerance -> Accuracy.TOO_LOW
            centsOffset > tolerance -> Accuracy.TOO_HIGH
            else -> Accuracy.NO_PITCH
        }

        return PitchFeedback(
            targetNote = targetNote,
            detectedNote = result.closestNote,
            detectedFrequency = result.frequency,
            centsOffset = centsOffset,
            semitonesOffset = semitonesOffset,
            accuracy = accuracy,
            isInTune = accuracy == Accuracy.CORRECT
        )
    }

    private fun medianSmooth(results: List<PitchResult>): PitchResult? {
        if (results.isEmpty()) return null
        if (results.size < 3) return results.last()

        val sorted = results.sortedBy { it.frequency }
        val median = sorted[sorted.size / 2]
        return median
    }

    override fun onCleared() {
        super.onCleared()
        cancelAll()
    }
}
