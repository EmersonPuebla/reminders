package com.example.reminders.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reminders.data.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val userPreferencesRepository: UserPreferencesRepository) : ViewModel() {

    val theme: StateFlow<Theme> = userPreferencesRepository.theme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Theme.SYSTEM
        )

    val serverAddress: StateFlow<String> = userPreferencesRepository.serverAddress
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ""
        )

    val serverPort: StateFlow<String> = userPreferencesRepository.serverPort
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ""
        )

    val syncEnabled: StateFlow<Boolean> = userPreferencesRepository.syncEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    val syncInterval: StateFlow<Int> = userPreferencesRepository.syncInterval
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 15
        )

    fun updateTheme(theme: Theme) {
        viewModelScope.launch {
            userPreferencesRepository.saveTheme(theme)
        }
    }

    fun saveConnectionDetails(address: String, port: String) {
        viewModelScope.launch {
            userPreferencesRepository.saveConnectionDetails(address, port)
        }
    }

    fun saveSyncSettings(enabled: Boolean, interval: Int) {
        viewModelScope.launch {
            userPreferencesRepository.saveSyncSettings(enabled, interval)
        }
    }
}
