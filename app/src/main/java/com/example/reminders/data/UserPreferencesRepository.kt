package com.example.reminders.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
        val USE_HTTPS = booleanPreferencesKey("use_https") // Nueva preferencia
        val SYNC_ENABLED = booleanPreferencesKey("sync_enabled")
        val SYNC_INTERVAL = intPreferencesKey("sync_interval")
        val SHOW_SYNC_BUTTON = booleanPreferencesKey("show_sync_button")
        val WEATHER_TEMP = stringPreferencesKey("weather_temp")
        val WEATHER_LOCATION = stringPreferencesKey("weather_location")
        val WEATHER_DESCRIPTION = stringPreferencesKey("weather_description")
        val WEATHER_ICON = stringPreferencesKey("weather_icon")
        val WEATHER_TIMESTAMP = longPreferencesKey("weather_timestamp")
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

    val useHttps: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.USE_HTTPS] ?: false
        }

    suspend fun saveConnectionDetails(address: String, port: String, useHttps: Boolean) {
        context.dataStore.edit {
            it[PreferencesKeys.SERVER_ADDRESS] = address
            it[PreferencesKeys.SERVER_PORT] = port
            it[PreferencesKeys.USE_HTTPS] = useHttps
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

    val showSyncButton: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SHOW_SYNC_BUTTON] ?: false
        }

    suspend fun saveShowSyncButton(show: Boolean) {
        context.dataStore.edit {
            it[PreferencesKeys.SHOW_SYNC_BUTTON] = show
        }
    }
    
    val weatherCache: Flow<WeatherCache?> = context.dataStore.data
        .map { preferences ->
            val temp = preferences[PreferencesKeys.WEATHER_TEMP]
            val location = preferences[PreferencesKeys.WEATHER_LOCATION]
            val description = preferences[PreferencesKeys.WEATHER_DESCRIPTION]
            val icon = preferences[PreferencesKeys.WEATHER_ICON]
            val timestamp = preferences[PreferencesKeys.WEATHER_TIMESTAMP]
            if (temp != null && location != null && description != null && icon != null && timestamp != null) {
                WeatherCache(temp, location, description, icon, timestamp)
            } else {
                null
            }
        }

    suspend fun saveWeatherCache(weatherCache: WeatherCache) {
        context.dataStore.edit {
            it[PreferencesKeys.WEATHER_TEMP] = weatherCache.temp
            it[PreferencesKeys.WEATHER_LOCATION] = weatherCache.location
            it[PreferencesKeys.WEATHER_DESCRIPTION] = weatherCache.description
            it[PreferencesKeys.WEATHER_ICON] = weatherCache.icon
            it[PreferencesKeys.WEATHER_TIMESTAMP] = weatherCache.timestamp
        }
    }
}

data class WeatherCache(
    val temp: String,
    val location: String,
    val description: String,
    val icon: String,
    val timestamp: Long
)
