package com.billbharo

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.billbharo.data.preferences.PreferencesManager
import com.billbharo.domain.utils.LocaleHelper
import com.billbharo.ui.navigation.NavGraph
import com.billbharo.ui.theme.BillBharoTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The main and only activity in the Bill Bharo application.
 *
 * This activity serves as the entry point for the app and hosts the Jetpack Compose content.
 * It is responsible for setting up the UI theme, navigation graph, and handling language
 * preference changes.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Injected instance of [PreferencesManager] for accessing user preferences, such as language.
     */
    @Inject
    lateinit var preferencesManager: PreferencesManager

    /**
     * Attaches the base context with the appropriate locale based on user preferences.
     * This is called before [onCreate] to ensure the correct resources are loaded.
     */
    override fun attachBaseContext(newBase: Context) {
        // The locale will be updated dynamically in onCreate, but this ensures initial setup.
        super.attachBaseContext(newBase)
    }

    /**
     * Called when the activity is first created.
     *
     * This method sets up the main content view using Jetpack Compose, initializes the theme,
     * sets up the navigation graph, and handles dynamic language changes.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load and apply the saved language preference at startup.
        lifecycleScope.launch {
            val language = preferencesManager.languageFlow.first()
            LocaleHelper.setLocale(this@MainActivity, language)
        }

        setContent {
            var currentLanguage by remember { mutableStateOf("") }

            // Observe changes in the language preference and recreate the activity to apply them.
            LaunchedEffect(Unit) {
                preferencesManager.languageFlow.collect { language ->
                    if (currentLanguage.isNotEmpty() && currentLanguage != language) {
                        LocaleHelper.setLocale(this@MainActivity, language)
                        recreate() // Recreating the activity is necessary to apply the new locale.
                    }
                    currentLanguage = language
                }
            }

            BillBharoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }
}
