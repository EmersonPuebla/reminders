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

    fun updateTheme(theme: Theme) {
        viewModelScope.launch {
            userPreferencesRepository.saveTheme(theme)
        }
    }
}
