package com.billbharo.di

import android.content.Context
import com.billbharo.domain.utils.VoiceRecognitionHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideVoiceRecognitionHelper(
        @ApplicationContext context: Context
    ): VoiceRecognitionHelper {
        return VoiceRecognitionHelper(context)
    }
}
