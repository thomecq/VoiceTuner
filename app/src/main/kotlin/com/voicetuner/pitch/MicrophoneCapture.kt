package com.voicetuner.pitch

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

class MicrophoneCapture {
    private var audioRecord: AudioRecord? = null

    companion object {
        const val SAMPLE_RATE = 44100
        const val BUFFER_SIZE = 2048
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_FLOAT
    }

    fun startCapture(): Flow<FloatArray> = flow {
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = maxOf(BUFFER_SIZE * 4, minBufferSize) // 4 bytes per float

        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return@flow
        }

        audioRecord = record
        record.startRecording()

        try {
            val buffer = FloatArray(BUFFER_SIZE)
            while (coroutineContext.isActive) {
                val read = record.read(buffer, 0, BUFFER_SIZE, AudioRecord.READ_BLOCKING)
                if (read > 0) {
                    emit(buffer.copyOf(read))
                }
            }
        } finally {
            record.stop()
            record.release()
            audioRecord = null
        }
    }.flowOn(Dispatchers.IO)

    fun stopCapture() {
        audioRecord?.let {
            try {
                it.stop()
            } catch (_: IllegalStateException) {}
        }
    }
}
