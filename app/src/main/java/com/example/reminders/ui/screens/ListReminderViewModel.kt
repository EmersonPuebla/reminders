package com.example.reminders.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reminders.data.Reminder
import com.example.reminders.data.RemindersRepository
import com.example.reminders.data.SyncResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReminderListViewModel(private val remindersRepository: RemindersRepository) : ViewModel() {

    val reminderListUiState: StateFlow<ReminderListUiState> =
        remindersRepository.getAllReminders().map { ReminderListUiState(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
                initialValue = ReminderListUiState()
            )

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

    suspend fun updateRemindersOrder(orderedIds: List<Int>) {
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
