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
    private val allSessionResults = mutableListOf<PitchResult>()
    private val medianWindowSize = 5

    companion object {
        const val DELAY_BEFORE_RECORDING_MS = 1400L
        const val RECORDING_DURATION_MS = 6000L
        const val MIN_SINGING_DURATION_MS = 800L
        const val SILENCE_AFTER_SINGING_MS = 500L
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
    private var singingStartTime: Long = 0L
    private var lastPitchDetectedTime: Long = 0L
    private var hasSungEnough: Boolean = false
    private var silenceCheckJob: Job? = null

    fun onNotePlayed(targetNote: Note) {
        // Cancel any ongoing recording/waiting
        cancelAll()

        currentTargetNote = targetNote
        _currentFeedback.value = null
        _currentPitchResult.value = null
        recentResults.clear()
        allSessionResults.clear()

        // Wait for the piano sound to naturally decay, then start recording
        _isWaitingToRecord.value = true
        captureJob = viewModelScope.launch {
            delay(DELAY_BEFORE_RECORDING_MS)
            _isWaitingToRecord.value = false
            startCapture(targetNote)
        }
    }

    private fun startCapture(targetNote: Note) {
        _isRecording.value = true
        singingStartTime = 0L
        lastPitchDetectedTime = 0L
        hasSungEnough = false

        // Hard auto-stop after max duration
        autoStopJob = viewModelScope.launch {
            delay(RECORDING_DURATION_MS)
            finishRecording()
        }

        captureJob = viewModelScope.launch {
            microphoneCapture.startCapture().collect { buffer ->
                val now = System.currentTimeMillis()
                val result = pitchDetector.detect(buffer)

                if (result != null && result.confidence > 0.5f) {
                    // Pitch detected — user is singing
                    if (singingStartTime == 0L) {
                        singingStartTime = now
                    }
                    lastPitchDetectedTime = now

                    // Check if sung long enough
                    if (now - singingStartTime >= MIN_SINGING_DURATION_MS) {
                        hasSungEnough = true
                    }

                    // Cancel any pending silence-finish
                    silenceCheckJob?.cancel()

                    allSessionResults.add(result)

                    recentResults.add(result)
                    if (recentResults.size > medianWindowSize) {
                        recentResults.removeAt(0)
                    }

                    val smoothedResult = medianSmooth(recentResults)
                    _currentPitchResult.value = smoothedResult

                    if (smoothedResult != null) {
                        _currentFeedback.value = createFeedback(targetNote, smoothedResult)
                    }
                } else if (hasSungEnough && lastPitchDetectedTime > 0L) {
                    // No pitch detected and user already sang enough —
                    // schedule finish after short silence
                    if (silenceCheckJob?.isActive != true) {
                        silenceCheckJob = viewModelScope.launch {
                            delay(SILENCE_AFTER_SINGING_MS)
                            finishRecording()
                        }
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
        silenceCheckJob?.cancel()
        silenceCheckJob = null
        _isRecording.value = false
        _isWaitingToRecord.value = false
        microphoneCapture.stopCapture()

        val target = currentTargetNote ?: return

        // Use stable result from entire session instead of last reading
        val stableResult = stableSessionResult(allSessionResults)
        val finalFeedback = if (stableResult != null) {
            createFeedback(target, stableResult)
        } else {
            _currentFeedback.value
        }

        if (finalFeedback != null) {
            _currentFeedback.value = finalFeedback
            val record = AttemptRecord(
                timestamp = System.currentTimeMillis(),
                targetNote = target,
                detectedNote = finalFeedback.detectedNote,
                centsOffset = finalFeedback.centsOffset,
                accuracy = finalFeedback.accuracy,
                isInTune = finalFeedback.isInTune
            )
            _attemptHistory.value = listOf(record) + _attemptHistory.value
        }
    }

    private fun cancelAll() {
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

    fun setTolerance(cents: Float) {
        _toleranceCents.value = cents.coerceIn(10f, 50f)
    }

    fun clearFeedback() {
        _currentFeedback.value = null
        _currentPitchResult.value = null
        recentResults.clear()
        allSessionResults.clear()
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

    /**
     * Trimmed median of all session results — drops top and bottom 20%
     * to eliminate voice cracks, onset noise, and trailing artifacts.
     */
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
        val median = sorted[sorted.size / 2]
        return median
    }

    override fun onCleared() {
        super.onCleared()
        cancelAll()
    }
}
