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
 * GeminiAudioTranscriber - Uses Android SpeechRecognizer (Step 1) + Gemini text parsing (Step 2)
 * Architecture: Voice → Android STT → Text → Gemini NLU → Structured Data
 * 
 * Supports Hindi, English, Marathi, and Hinglish
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
     * STEP 1: Use Android SpeechRecognizer to convert voice to text
     * Returns a Flow emitting transcription states
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
     * Stop speech recognition
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
 * Result states for voice transcription flow.
 */
sealed class VoiceTranscriptionResult {
    object Ready : VoiceTranscriptionResult()
    object Recording : VoiceTranscriptionResult()
    object Processing : VoiceTranscriptionResult()
    data class Success(val transcription: String) : VoiceTranscriptionResult()
    data class Error(val message: String) : VoiceTranscriptionResult()
}

