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

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var preferencesManager: PreferencesManager
    
    override fun attachBaseContext(newBase: Context) {
        // This will be updated when language preference is loaded
        super.attachBaseContext(newBase)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Load and apply language preference
        lifecycleScope.launch {
            val language = preferencesManager.languageFlow.first()
            LocaleHelper.setLocale(this@MainActivity, language)
        }
        
        setContent {
            var currentLanguage by remember { mutableStateOf("") }
            
            // Observe language changes
            LaunchedEffect(Unit) {
                preferencesManager.languageFlow.collect { language ->
                    if (currentLanguage.isNotEmpty() && currentLanguage != language) {
                        // Language changed, apply new locale
                        LocaleHelper.setLocale(this@MainActivity, language)
                        recreate() // Recreate activity to apply language change
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
