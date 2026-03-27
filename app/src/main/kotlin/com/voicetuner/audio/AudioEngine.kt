package com.voicetuner.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.voicetuner.model.Chord
import com.voicetuner.model.Note
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AudioEngine {
    private val synthesizer = PianoSynthesizer()
    private val playbackScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var currentJob: Job? = null
    private var audioTrack: AudioTrack? = null

    private val noteCache = HashMap<Int, ShortArray>()

    fun precomputeNotes(notes: List<Note>) {
        for (note in notes) {
            if (note.midiNumber !in noteCache) {
                noteCache[note.midiNumber] = synthesizer.synthesizeNote(note.frequency)
            }
        }
    }

    fun playNote(note: Note) {
        stop()
        currentJob = playbackScope.launch {
            val samples = noteCache[note.midiNumber]
                ?: synthesizer.synthesizeNote(note.frequency).also {
                    noteCache[note.midiNumber] = it
                }
            playSamples(samples)
        }
    }

    fun playChord(chord: Chord) {
        stop()
        currentJob = playbackScope.launch {
            val frequencies = chord.notes.map { it.frequency }
            val samples = synthesizer.synthesizeChord(frequencies)
            playSamples(samples)
        }
    }

    fun stop() {
        currentJob?.cancel()
        currentJob = null
        audioTrack?.let {
            try {
                it.stop()
                it.release()
            } catch (_: IllegalStateException) {}
        }
        audioTrack = null
    }

    fun release() {
        stop()
        noteCache.clear()
    }

    private fun playSamples(samples: ShortArray) {
        val bufferSize = samples.size * 2 // 2 bytes per short

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(AudioConfig.SAMPLE_RATE)
                    .setChannelMask(AudioConfig.CHANNEL_CONFIG_OUT)
                    .setEncoding(AudioConfig.AUDIO_FORMAT)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()

        audioTrack = track
        track.write(samples, 0, samples.size)
        track.play()
    }
}
