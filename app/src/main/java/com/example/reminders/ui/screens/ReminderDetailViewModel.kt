package com.example.reminders.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reminders.data.Reminder
import com.example.reminders.data.RemindersRepository
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderDetailViewModel(private val remindersRepository: RemindersRepository, private val savedStateHandle: SavedStateHandle) : ViewModel() {

    var reminderUiState by mutableStateOf(ReminderUiState())
        private set

    private val reminderId: Int? = savedStateHandle["reminderId"]

    init {
        if (reminderId != null) {
            viewModelScope.launch {
                reminderUiState = remindersRepository.getReminder(reminderId)
                    .filterNotNull()
                    .first()
                    .toReminderUiState()
            }
        }
    }

    fun updateUiState(newReminderUiState: ReminderUiState) {
        reminderUiState = newReminderUiState
    }

    suspend fun saveReminder() {
        remindersRepository.insert(reminderUiState.toReminder())
    }
}

data class ReminderUiState(
    val id: Int = 0,
    val title: String = "",
    val description: String = "",
    val date: Long = 0L,
    val notify: Boolean = false,
    val audioUris: List<String> = listOf()
)

fun ReminderUiState.toReminder(): Reminder = Reminder(
    id = id,
    title = title,
    description = description,
    date = date,
    notify = notify,
    audioUris = audioUris
)

fun Reminder.toReminderUiState(): ReminderUiState = ReminderUiState(
    id = id,
    title = title,
    description = description,
    date = date,
    notify = notify,
    audioUris = audioUris
)
