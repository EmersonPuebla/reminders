package com.example.reminders.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.reminders.ui.screens.Theme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME = stringPreferencesKey("theme")
        val SERVER_ADDRESS = stringPreferencesKey("server_address")
        val SERVER_PORT = stringPreferencesKey("server_port")
        val SYNC_ENABLED = booleanPreferencesKey("sync_enabled")
        val SYNC_INTERVAL = intPreferencesKey("sync_interval")
    }

    val theme: Flow<Theme> = context.dataStore.data
        .map { preferences ->
            val themeName = preferences[PreferencesKeys.THEME] ?: Theme.SYSTEM.name
            Theme.valueOf(themeName)
        }

    suspend fun saveTheme(theme: Theme) {
        context.dataStore.edit {
            it[PreferencesKeys.THEME] = theme.name
        }
    }

    val serverAddress: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SERVER_ADDRESS] ?: ""
        }

    val serverPort: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SERVER_PORT] ?: ""
        }

    suspend fun saveConnectionDetails(address: String, port: String) {
        context.dataStore.edit {
            it[PreferencesKeys.SERVER_ADDRESS] = address
            it[PreferencesKeys.SERVER_PORT] = port
        }
    }

    val syncEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SYNC_ENABLED] ?: false
        }

    val syncInterval: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SYNC_INTERVAL] ?: 15
        }

    suspend fun saveSyncSettings(enabled: Boolean, interval: Int) {
        context.dataStore.edit {
            it[PreferencesKeys.SYNC_ENABLED] = enabled
            it[PreferencesKeys.SYNC_INTERVAL] = interval
        }
    }
}
