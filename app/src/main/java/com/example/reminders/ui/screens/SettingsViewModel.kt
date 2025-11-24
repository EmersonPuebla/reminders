package com.example.reminders.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reminders.data.UserPreferencesRepository
import com.example.reminders.data.network.ApiService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val apiService: ApiService
) : ViewModel() {

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

    val useHttps: StateFlow<Boolean> = userPreferencesRepository.useHttps
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
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

    val showSyncButton: StateFlow<Boolean> = userPreferencesRepository.showSyncButton
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    fun updateTheme(theme: Theme) {
        viewModelScope.launch {
            userPreferencesRepository.saveTheme(theme)
        }
    }

    fun saveConnectionDetails(address: String, port: String, useHttps: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.saveConnectionDetails(address, port, useHttps)
        }
    }

    fun saveSyncSettings(enabled: Boolean, interval: Int) {
        viewModelScope.launch {
            userPreferencesRepository.saveSyncSettings(enabled, interval)
        }
    }

    fun saveShowSyncButton(show: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.saveShowSyncButton(show)
        }
    }

    suspend fun testConnection(): Boolean {
        return try {
            android.util.Log.d("SettingsViewModel", "Iniciando testConnection...")
            val response = apiService.testConnection()
            android.util.Log.d("SettingsViewModel", "Respuesta: isSuccessful=${response.isSuccessful}, code=${response.code()}")
            val success = response.isSuccessful && response.code() == 200
            if (success) {
                android.util.Log.d("SettingsViewModel", "Conexión exitosa a la API")
            } else {
                android.util.Log.e("SettingsViewModel", "Error en conexión: ${response.code()}")
            }
            success
        } catch (e: Exception) {
            android.util.Log.e("SettingsViewModel", "Excepción en testConnection: ${e.message}", e)
            false
        }
    }
}
