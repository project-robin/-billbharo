package com.billbharo.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billbharo.data.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Represents the UI state for the [SettingsScreen].
 *
 * @property selectedLanguage The currently selected language code.
 * @property showLanguageDialog A flag to control the visibility of the language selection dialog.
 * @property showRestartMessage A flag to control the visibility of the "restart required" message.
 */
data class SettingsUiState(
    val selectedLanguage: String = PreferencesManager.LANGUAGE_HINDI,
    val showLanguageDialog: Boolean = false,
    val showRestartMessage: Boolean = false
)

/**
 * The ViewModel for the [SettingsScreen].
 *
 * This class is responsible for managing the application's settings, primarily the language preference.
 * It interacts with [PreferencesManager] to load and save the selected language.
 *
 * @property preferencesManager The manager for handling application preferences.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadLanguagePreference()
    }

    /**
     * Loads the current language preference from [PreferencesManager] and updates the UI state.
     */
    private fun loadLanguagePreference() {
        viewModelScope.launch {
            preferencesManager.languageFlow.collect { language ->
                _uiState.value = _uiState.value.copy(selectedLanguage = language)
            }
        }
    }

    /**
     * Shows the language selection dialog.
     */
    fun showLanguageDialog() {
        _uiState.value = _uiState.value.copy(showLanguageDialog = true)
    }

    /**
     * Hides the language selection dialog.
     */
    fun hideLanguageDialog() {
        _uiState.value = _uiState.value.copy(showLanguageDialog = false)
    }

    /**
     * Updates the application's language preference.
     *
     * @param languageCode The new language code to be saved.
     */
    fun selectLanguage(languageCode: String) {
        viewModelScope.launch {
            preferencesManager.setLanguage(languageCode)
            _uiState.value = _uiState.value.copy(
                selectedLanguage = languageCode,
                showLanguageDialog = false,
                showRestartMessage = true
            )
        }
    }

    /**
     * Dismisses the "restart required" message.
     */
    fun dismissRestartMessage() {
        _uiState.value = _uiState.value.copy(showRestartMessage = false)
    }
}
