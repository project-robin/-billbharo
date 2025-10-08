package com.billbharo.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Extension property for DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bill_bharo_preferences")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val LANGUAGE_KEY = stringPreferencesKey("app_language")
        const val LANGUAGE_ENGLISH = "en"
        const val LANGUAGE_HINDI = "hi"
    }

    /**
     * Get the currently selected language
     * Defaults to Hindi if not set
     */
    val languageFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[LANGUAGE_KEY] ?: LANGUAGE_HINDI
    }

    /**
     * Set the app language
     * @param languageCode The language code (en, hi)
     */
    suspend fun setLanguage(languageCode: String) {
        dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = languageCode
        }
    }

    /**
     * Get the current language synchronously
     * Returns Hindi by default
     */
    suspend fun getCurrentLanguage(): String {
        var currentLanguage = LANGUAGE_HINDI
        dataStore.data.collect { preferences ->
            currentLanguage = preferences[LANGUAGE_KEY] ?: LANGUAGE_HINDI
        }
        return currentLanguage
    }
}
