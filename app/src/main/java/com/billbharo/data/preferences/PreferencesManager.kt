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

/**
 * Extension property for [Context] to provide a singleton instance of [DataStore].
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bill_bharo_preferences")

/**
 * Manages application preferences using Jetpack DataStore.
 *
 * This class provides methods to get and set user preferences, such as the app language.
 *
 * @property context The application context, provided by Hilt.
 */
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        /** [Preferences.Key] for storing the app language. */
        val LANGUAGE_KEY = stringPreferencesKey("app_language")

        /** Constant for the English language code. */
        const val LANGUAGE_ENGLISH = "en"

        /** Constant for the Hindi language code. */
        const val LANGUAGE_HINDI = "hi"
    }

    /**
     * A [Flow] that emits the currently selected language.
     *
     * It defaults to Hindi ([LANGUAGE_HINDI]) if no language is set.
     */
    val languageFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[LANGUAGE_KEY] ?: LANGUAGE_HINDI
    }

    /**
     * Sets the application language.
     *
     * @param languageCode The language code to set (e.g., "en", "hi").
     */
    suspend fun setLanguage(languageCode: String) {
        dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = languageCode
        }
    }

    /**
     * Retrieves the current language synchronously.
     *
     * Note: This function is suspendable and should be called from a coroutine.
     *
     * @return The current language code, defaulting to Hindi ([LANGUAGE_HINDI]).
     */
    suspend fun getCurrentLanguage(): String {
        var currentLanguage = LANGUAGE_HINDI
        dataStore.data.collect { preferences ->
            currentLanguage = preferences[LANGUAGE_KEY] ?: LANGUAGE_HINDI
        }
        return currentLanguage
    }
}
