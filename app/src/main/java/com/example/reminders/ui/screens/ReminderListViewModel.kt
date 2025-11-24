package com.example.reminders.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reminders.data.Reminder
import com.example.reminders.data.RemindersRepository
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

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}

data class ReminderListUiState(
    val itemList: List<Reminder> = listOf()
)
