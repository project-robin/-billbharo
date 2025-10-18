package com.billbharo.di

import android.content.Context
import com.billbharo.domain.utils.AudioRecorder
import com.billbharo.domain.utils.GeminiAudioTranscriber
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // GeminiAudioTranscriber now uses Android SpeechRecognizer
    // It's injected automatically via @Inject constructor with @ApplicationContext
    // No manual DI needed - Hilt handles it
}
