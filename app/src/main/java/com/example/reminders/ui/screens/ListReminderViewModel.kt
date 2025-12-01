package com.example.reminders.ui.screens

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reminders.data.LocationProvider
import com.example.reminders.data.Reminder
import com.example.reminders.data.RemindersRepository
import com.example.reminders.data.SyncResult
import com.example.reminders.data.UserPreferencesRepository
import com.example.reminders.data.WeatherCache
import com.example.reminders.data.network.WeatherResponse
import com.example.reminders.data.network.WeatherService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReminderListViewModel(
    private val remindersRepository: RemindersRepository,
    private val weatherService: WeatherService,
    private val locationProvider: LocationProvider,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val reminderListUiState: StateFlow<ReminderListUiState> =
        remindersRepository.getAllReminders().map { ReminderListUiState(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
                initialValue = ReminderListUiState()
            )

    private val _weatherUiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val weatherUiState: StateFlow<WeatherUiState> = _weatherUiState.asStateFlow()

    fun fetchWeather() {
        viewModelScope.launch {
            val currentTime = System.currentTimeMillis()
            val cachedWeather = userPreferencesRepository.weatherCache.first()

            if (cachedWeather != null && (currentTime - cachedWeather.timestamp < 1800000)) { // 30 minutes
                _weatherUiState.value = WeatherUiState.Success(
                    WeatherResponse(
                        main = com.example.reminders.data.network.Main(cachedWeather.temp.toDouble()),
                        weather = listOf(com.example.reminders.data.network.Weather(cachedWeather.description, cachedWeather.icon)),
                        name = cachedWeather.location
                    )
                )
                return@launch
            }

            _weatherUiState.value = WeatherUiState.Loading
            try {
                var location = locationProvider.getCurrentLocation()
                var isFallback = false
                if (location == null) {
                    isFallback = true
                    // Fallback to a default location (San Bernardo)
                    location = Location("Default").apply {
                        latitude = -33.6
                        longitude = -70.716667
                    }
                }
                val weather = weatherService.getCurrentWeather(location.latitude, location.longitude, "67016a2f4d7e1883cc7736b077b20788")
                
                val finalWeather = if (isFallback) {
                    weather.copy(name = "San Bernardo")
                } else {
                    weather
                }
                
                userPreferencesRepository.saveWeatherCache(
                    WeatherCache(
                        temp = finalWeather.main.temp.toString(),
                        location = finalWeather.name,
                        description = finalWeather.weather[0].description,
                        icon = finalWeather.weather[0].icon,
                        timestamp = currentTime
                    )
                )

                _weatherUiState.value = WeatherUiState.Success(finalWeather)
            } catch (e: Exception) {
                _weatherUiState.value = WeatherUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun deleteReminders(ids: List<Int>) {
        viewModelScope.launch {
            remindersRepository.deleteReminders(ids)
        }
    }

    fun syncRemindersAndFetchMissingAsync(onResult: (SyncResult) -> Unit) {
        viewModelScope.launch {
            val result = remindersRepository.syncRemindersAndFetchMissing()
            onResult(result)
        }
    }

    fun updateRemindersOrder(orderedIds: List<Int>) {
        viewModelScope.launch {
            val currentReminders = reminderListUiState.value.itemList

            // Crear mapa de ID a Reminder para búsqueda rápida
            val reminderMap = currentReminders.associateBy { it.id }

            // Actualizar sortOrder basado en la nueva posición
            val updatedReminders = orderedIds.mapIndexed { index, id ->
                reminderMap[id]?.copy(sortOrder = index)
            }.filterNotNull()

            // Guardar en la base de datos
            updatedReminders.forEach { reminder ->
                remindersRepository.update(reminder)
            }
        }
    }

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}

data class ReminderListUiState(
    val itemList: List<Reminder> = listOf()
)

sealed interface WeatherUiState {
    data class Success(val weather: com.example.reminders.data.network.WeatherResponse) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
    object Loading : WeatherUiState
}
