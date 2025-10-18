package com.billbharo.domain.utils

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AudioRecorder - Captures raw PCM audio for Gemini transcription.
 * Configured for optimal Gemini 2.0 Flash audio processing.
 */
@Singleton
class AudioRecorder @Inject constructor() {

    companion object {
        // Gemini 2.0 Flash recommended audio format
        private const val SAMPLE_RATE = 16000 // 16 kHz
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
        private const val MAX_RECORDING_DURATION_MS = 30_000L // 30 seconds max
    }

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val bufferSize = AudioRecord.getMinBufferSize(
        SAMPLE_RATE,
        CHANNEL_CONFIG,
        AUDIO_ENCODING
    )

    /**
     * Start recording audio.
     * @return ByteArray of PCM audio data (16-bit, 16kHz, mono)
     * @throws AudioRecordingException if recording fails
     */
    suspend fun recordAudio(): ByteArray = withContext(Dispatchers.IO) {
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            throw AudioRecordingException("Invalid buffer size for audio recording")
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_ENCODING,
                bufferSize * 4 // 4x buffer for stability
            )

            val state = audioRecord?.state
            if (state != AudioRecord.STATE_INITIALIZED) {
                throw AudioRecordingException("AudioRecord initialization failed. State: $state")
            }

            audioRecord?.startRecording()
            isRecording = true

            val outputStream = ByteArrayOutputStream()
            val buffer = ByteArray(bufferSize)
            val startTime = System.currentTimeMillis()

            while (isRecording && (System.currentTimeMillis() - startTime) < MAX_RECORDING_DURATION_MS) {
                val readBytes = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                
                if (readBytes > 0) {
                    outputStream.write(buffer, 0, readBytes)
                } else if (readBytes == AudioRecord.ERROR_INVALID_OPERATION) {
                    throw AudioRecordingException("Invalid audio recording operation")
                } else if (readBytes == AudioRecord.ERROR_BAD_VALUE) {
                    throw AudioRecordingException("Bad audio recording parameters")
                }
            }

            val pcmData = outputStream.toByteArray()
            
            if (pcmData.isEmpty()) {
                throw AudioRecordingException("No audio data captured")
            }

            // Convert raw PCM to WAV format (required by Gemini)
            convertPcmToWav(pcmData)
        } catch (e: SecurityException) {
            throw AudioRecordingException("Microphone permission not granted", e)
        } catch (e: Exception) {
            if (e is AudioRecordingException) throw e
            throw AudioRecordingException("Audio recording failed: ${e.message}", e)
        } finally {
            stopRecording()
        }
    }

    /**
     * Stop recording immediately.
     */
    fun stopRecording() {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // Silently ignore cleanup errors
        } finally {
            audioRecord = null
        }
    }

    /**
     * Check if recording is currently active.
     */
    fun isRecording(): Boolean = isRecording

    /**
     * Convert raw PCM data to WAV format with proper headers.
     * Gemini requires WAV/MP3/FLAC - raw PCM is not supported.
     */
    private fun convertPcmToWav(pcmData: ByteArray): ByteArray {
        val wavOutputStream = ByteArrayOutputStream()
        
        // WAV header structure
        val totalDataLen = pcmData.size + 36
        val channels = 1 // Mono
        val byteRate = SAMPLE_RATE * channels * 2 // 16-bit = 2 bytes
        
        // Write WAV header
        wavOutputStream.write("RIFF".toByteArray()) // ChunkID
        wavOutputStream.write(intToByteArray(totalDataLen), 0, 4) // ChunkSize
        wavOutputStream.write("WAVE".toByteArray()) // Format
        
        // Subchunk1 (fmt)
        wavOutputStream.write("fmt ".toByteArray()) // Subchunk1ID
        wavOutputStream.write(intToByteArray(16), 0, 4) // Subchunk1Size (16 for PCM)
        wavOutputStream.write(shortToByteArray(1), 0, 2) // AudioFormat (1 = PCM)
        wavOutputStream.write(shortToByteArray(channels.toShort()), 0, 2) // NumChannels
        wavOutputStream.write(intToByteArray(SAMPLE_RATE), 0, 4) // SampleRate
        wavOutputStream.write(intToByteArray(byteRate), 0, 4) // ByteRate
        wavOutputStream.write(shortToByteArray(4), 0, 2) // BlockAlign (channels * bitsPerSample/8)
        wavOutputStream.write(shortToByteArray(16), 0, 2) // BitsPerSample
        
        // Subchunk2 (data)
        wavOutputStream.write("data".toByteArray()) // Subchunk2ID
        wavOutputStream.write(intToByteArray(pcmData.size), 0, 4) // Subchunk2Size
        wavOutputStream.write(pcmData) // Actual audio data
        
        return wavOutputStream.toByteArray()
    }
    
    /**
     * Convert int to little-endian byte array.
     */
    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            (value shr 8 and 0xFF).toByte(),
            (value shr 16 and 0xFF).toByte(),
            (value shr 24 and 0xFF).toByte()
        )
    }
    
    /**
     * Convert short to little-endian byte array.
     */
    private fun shortToByteArray(value: Short): ByteArray {
        return byteArrayOf(
            (value.toInt() and 0xFF).toByte(),
            (value.toInt() shr 8 and 0xFF).toByte()
        )
    }
}

/**
 * Exception thrown when audio recording fails.
 */
class AudioRecordingException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
