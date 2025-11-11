package com.billbharo.domain.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

/**
 * A utility object for managing application locale and language settings.
 *
 * This object provides helper functions to set the app's current locale, retrieve the
 * current language, and get the display name for a given language code.
 */
object LocaleHelper {
    /**
     * Sets the application's locale to the specified language.
     *
     * This function updates the app's configuration to use the new locale, ensuring that
     * all resources are displayed in the selected language.
     *
     * @param context The application context.
     * @param languageCode The language code to set (e.g., "en", "hi").
     * @return A new [Context] with the updated locale configuration.
     */
    fun setLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }

    /**
     * Retrieves the language code of the current locale.
     *
     * @param context The application context.
     * @return The current language code (e.g., "en", "hi").
     */
    fun getCurrentLanguage(context: Context): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0].language
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale.language
        }
    }

    /**
     * Gets the display name for a given language code.
     *
     * @param languageCode The language code (e.g., "en", "hi").
     * @return The localized display name of the language (e.g., "English", "हिंदी").
     */
    fun getLanguageDisplayName(languageCode: String): String {
        return when (languageCode) {
            "en" -> "English"
            "hi" -> "हिंदी"
            else -> "English"
        }
    }
}
