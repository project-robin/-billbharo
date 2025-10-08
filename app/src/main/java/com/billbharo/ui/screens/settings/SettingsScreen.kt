package com.billbharo.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.billbharo.R
import com.billbharo.data.preferences.PreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Language Settings Section
            SettingsSection(title = stringResource(R.string.language_settings))
            
            LanguageSettingItem(
                selectedLanguage = uiState.selectedLanguage,
                onLanguageClick = { viewModel.showLanguageDialog() }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))
        }

        // Language Selection Dialog
        if (uiState.showLanguageDialog) {
            LanguageSelectionDialog(
                currentLanguage = uiState.selectedLanguage,
                onLanguageSelected = { languageCode ->
                    viewModel.selectLanguage(languageCode)
                },
                onDismiss = { viewModel.hideLanguageDialog() }
            )
        }

        // Restart Message Snackbar
        if (uiState.showRestartMessage) {
            RestartMessageSnackbar(
                onDismiss = { viewModel.dismissRestartMessage() }
            )
        }
    }
}

@Composable
fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
fun LanguageSettingItem(
    selectedLanguage: String,
    onLanguageClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onLanguageClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.language),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = getLanguageDisplayName(selectedLanguage),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun LanguageSelectionDialog(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.select_language),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                LanguageOption(
                    language = PreferencesManager.LANGUAGE_ENGLISH,
                    languageName = stringResource(R.string.english),
                    isSelected = currentLanguage == PreferencesManager.LANGUAGE_ENGLISH,
                    onClick = { onLanguageSelected(PreferencesManager.LANGUAGE_ENGLISH) }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LanguageOption(
                    language = PreferencesManager.LANGUAGE_HINDI,
                    languageName = stringResource(R.string.hindi),
                    isSelected = currentLanguage == PreferencesManager.LANGUAGE_HINDI,
                    onClick = { onLanguageSelected(PreferencesManager.LANGUAGE_HINDI) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun LanguageOption(
    language: String,
    languageName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = languageName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun RestartMessageSnackbar(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Snackbar(
            action = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.ok))
                }
            },
            modifier = Modifier.padding(8.dp)
        ) {
            Text(stringResource(R.string.restart_app_message))
        }
    }
}

@Composable
fun getLanguageDisplayName(languageCode: String): String {
    return when (languageCode) {
        PreferencesManager.LANGUAGE_ENGLISH -> stringResource(R.string.english)
        PreferencesManager.LANGUAGE_HINDI -> stringResource(R.string.hindi)
        else -> stringResource(R.string.english)
    }
}
