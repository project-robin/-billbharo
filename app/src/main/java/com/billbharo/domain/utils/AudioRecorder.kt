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
 * Handles the recording of raw PCM audio data, optimized for Gemini 2.0 Flash transcription.
 *
 * This class manages the [AudioRecord] instance, handles the recording process, and converts
 * the captured audio to the WAV format required by the Gemini API.
 */
@Singleton
class AudioRecorder @Inject constructor() {

    companion object {
        /** The recommended sample rate for Gemini 2.0 Flash (16 kHz). */
        private const val SAMPLE_RATE = 16000

        /** The channel configuration for mono audio. */
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO

        /** The audio encoding format (16-bit PCM). */
        private const val AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT

        /** The maximum duration for a single recording (30 seconds). */
        private const val MAX_RECORDING_DURATION_MS = 30_000L
    }

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val bufferSize = AudioRecord.getMinBufferSize(
        SAMPLE_RATE,
        CHANNEL_CONFIG,
        AUDIO_ENCODING
    )

    /**
     * Starts the audio recording process.
     *
     * Captures raw PCM audio, stops after a maximum duration, and converts the data to WAV format.
     *
     * @return A [ByteArray] containing the audio data in WAV format.
     * @throws AudioRecordingException if the recording fails for any reason.
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
     * Stops the audio recording immediately and releases resources.
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
     * Checks if the audio recorder is currently active.
     *
     * @return `true` if recording is in progress, `false` otherwise.
     */
    fun isRecording(): Boolean = isRecording

    /**
     * Converts raw PCM audio data to WAV format by adding the necessary headers.
     *
     * @param pcmData The raw PCM audio data.
     * @return A [ByteArray] containing the audio data in WAV format.
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
     * Converts an integer to a little-endian byte array.
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
     * Converts a short to a little-endian byte array.
     */
    private fun shortToByteArray(value: Short): ByteArray {
        return byteArrayOf(
            (value.toInt() and 0xFF).toByte(),
            (value.toInt() shr 8 and 0xFF).toByte()
        )
    }
}

/**
 * Custom exception for handling audio recording errors.
 *
 * @param message A descriptive error message.
 * @param cause The underlying cause of the exception (optional).
 */
class AudioRecordingException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
