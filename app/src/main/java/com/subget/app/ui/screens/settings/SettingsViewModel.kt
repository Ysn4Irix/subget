package com.subget.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.subget.app.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val apiKey: String = "",
    val isKeyVisible: Boolean = false,
    val preferredLanguages: String = "en",
    val themeMode: String = SettingsRepository.THEME_LIGHT,
    val saveMessage: String? = null
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            apiKey = settingsRepository.getApiKey(),
            preferredLanguages = settingsRepository.getPreferredLanguages(),
            themeMode = settingsRepository.getThemeMode()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onApiKeyChanged(newKey: String) {
        _uiState.value = _uiState.value.copy(apiKey = newKey, saveMessage = null)
    }

    fun toggleKeyVisibility() {
        _uiState.value = _uiState.value.copy(isKeyVisible = !_uiState.value.isKeyVisible)
    }

    fun onLanguagesChanged(newLanguages: String) {
        _uiState.value = _uiState.value.copy(preferredLanguages = newLanguages, saveMessage = null)
    }

    fun onThemeModeChanged(newMode: String) {
        settingsRepository.setThemeMode(newMode)
        _uiState.value = _uiState.value.copy(themeMode = newMode)
    }

    fun saveSettings() {
        val currentKey = _uiState.value.apiKey.trim()
        val currentLanguages = _uiState.value.preferredLanguages.trim()
        val currentThemeMode = _uiState.value.themeMode

        settingsRepository.setApiKey(currentKey)
        settingsRepository.setPreferredLanguages(currentLanguages)
        settingsRepository.setThemeMode(currentThemeMode)
        settingsRepository.setFirstLaunchComplete()

        _uiState.value = _uiState.value.copy(
            saveMessage = "Settings saved securely"
        )
    }

    fun clearSaveMessage() {
        _uiState.value = _uiState.value.copy(saveMessage = null)
    }

    class Factory(private val settingsRepository: SettingsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(settingsRepository) as T
        }
    }
}
