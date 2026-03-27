package com.voicetuner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicetuner.audio.AudioEngine
import com.voicetuner.audio.NoteFrequencies
import com.voicetuner.model.Chord
import com.voicetuner.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PianoViewModel : ViewModel() {
    private val audioEngine = AudioEngine()

    private val _pressedKeys = MutableStateFlow<Set<Int>>(emptySet())
    val pressedKeys: StateFlow<Set<Int>> = _pressedKeys.asStateFlow()

    private val _lastPlayedNote = MutableStateFlow<Note?>(null)
    val lastPlayedNote: StateFlow<Note?> = _lastPlayedNote.asStateFlow()

    private val _playCounter = MutableStateFlow(0)
    val playCounter: StateFlow<Int> = _playCounter.asStateFlow()

    private val _isChordMode = MutableStateFlow(false)
    val isChordMode: StateFlow<Boolean> = _isChordMode.asStateFlow()

    val allNotes: List<Note> = NoteFrequencies.getAllNotes()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            audioEngine.precomputeNotes(allNotes)
        }
    }

    fun onKeyPress(note: Note) {
        if (_isChordMode.value) {
            val currentKeys = _pressedKeys.value.toMutableSet()
            if (note.midiNumber in currentKeys) {
                currentKeys.remove(note.midiNumber)
            } else {
                currentKeys.add(note.midiNumber)
            }
            _pressedKeys.value = currentKeys

            if (currentKeys.isNotEmpty()) {
                val chordNotes = currentKeys.mapNotNull { NoteFrequencies.getNote(it) }
                audioEngine.playChord(Chord(chordNotes))
                _lastPlayedNote.value = chordNotes.firstOrNull()
            }
        } else {
            _pressedKeys.value = setOf(note.midiNumber)
            _lastPlayedNote.value = note
            _playCounter.value++
            audioEngine.playNote(note)
        }
    }

    fun onKeyRelease(note: Note) {
        if (!_isChordMode.value) {
            _pressedKeys.value = emptySet()
        }
    }

    fun toggleChordMode() {
        _isChordMode.value = !_isChordMode.value
        _pressedKeys.value = emptySet()
    }

    fun stopPlayback() {
        audioEngine.stop()
    }

    fun clearChord() {
        _pressedKeys.value = emptySet()
        audioEngine.stop()
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }
}
