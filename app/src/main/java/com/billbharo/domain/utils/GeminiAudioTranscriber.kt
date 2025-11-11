package com.billbharo.domain.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles the first step of the voice-to-invoice process: transcribing audio to text.
 *
 * This class uses the Android [SpeechRecognizer] to convert spoken language into a text string.
 * It supports multiple languages and provides a [Flow]-based API to report the state of the
 * transcription process.
 *
 * The overall architecture is a two-step process:
 * 1.  **Voice → Android STT → Text** (handled by this class)
 * 2.  **Text → Gemini NLU → Structured Data** (handled by [GeminiInvoiceParser])
 *
 * @property context The application context, provided by Hilt.
 */
@Singleton
class GeminiAudioTranscriber @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "GeminiTranscriber"
    }

    private var speechRecognizer: SpeechRecognizer? = null

    /**
     * Starts the audio transcription process using the Android [SpeechRecognizer].
     *
     * This function returns a [Flow] that emits [VoiceTranscriptionResult] states, allowing the UI
     * to react to the different stages of speech recognition (e.g., ready, recording, success, error).
     *
     * @param language The language code for speech recognition (e.g., "hi-IN" for Hindi).
     * @return A [Flow] of [VoiceTranscriptionResult] representing the transcription state.
     */
    fun transcribeAudio(language: String = "hi-IN"): Flow<VoiceTranscriptionResult> = callbackFlow {
        Log.d(TAG, "Starting speech recognition for language: $language")

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            trySend(VoiceTranscriptionResult.Error("Speech recognition not available on this device"))
            close()
            return@callbackFlow
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak item, quantity, and price...")
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
                trySend(VoiceTranscriptionResult.Ready)
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Speech started")
                trySend(VoiceTranscriptionResult.Recording)
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(TAG, "Speech ended, processing...")
                trySend(VoiceTranscriptionResult.Processing)
            }

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission denied"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Please speak clearly."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected"
                    else -> "Unknown error: $error"
                }
                Log.e(TAG, "Speech recognition error: $errorMessage (code: $error)")
                trySend(VoiceTranscriptionResult.Error(errorMessage))
                close()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val transcription = matches[0]
                    Log.d(TAG, "Speech recognized: $transcription")
                    trySend(VoiceTranscriptionResult.Success(transcription))
                } else {
                    Log.w(TAG, "No recognition results")
                    trySend(VoiceTranscriptionResult.Error("No speech detected"))
                }
                close()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    Log.d(TAG, "Partial: ${matches[0]}")
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        try {
            trySend(VoiceTranscriptionResult.Ready)
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recognition", e)
            trySend(VoiceTranscriptionResult.Error("Failed to start: ${e.message}"))
            close()
        }

        awaitClose {
            Log.d(TAG, "Stopping speech recognition")
            stopRecording()
        }
    }

    /**
     * Stops the speech recognition process and releases resources.
     *
     * This should be called when the transcription is no longer needed, such as when the
     * user navigates away from the screen.
     */
    fun stopRecording() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
            Log.d(TAG, "Speech recognition stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recognition", e)
        }
    }
}

/**
 * Represents the different states of the voice transcription process.
 */
sealed class VoiceTranscriptionResult {
    /** The speech recognizer is ready to start listening. */
    object Ready : VoiceTranscriptionResult()

    /** The speech recognizer is currently recording audio. */
    object Recording : VoiceTranscriptionResult()

    /** The speech recognizer has finished recording and is processing the audio. */
    object Processing : VoiceTranscriptionResult()

    /**
     * The speech recognizer successfully transcribed the audio.
     * @property transcription The transcribed text.
     */
    data class Success(val transcription: String) : VoiceTranscriptionResult()

    /**
     * An error occurred during the transcription process.
     * @property message A descriptive error message.
     */
    data class Error(val message: String) : VoiceTranscriptionResult()
}

