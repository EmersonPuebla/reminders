package com.example.reminders.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reminders.data.RemindersRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ViewReminderViewModel(savedStateHandle: SavedStateHandle, private val remindersRepository: RemindersRepository) : ViewModel() {

    private val reminderId: Int = checkNotNull(savedStateHandle["reminderId"])

    val uiState: StateFlow<ReminderUiState?> =
        remindersRepository.getReminder(reminderId)
            .map { reminder ->
                if (reminder == null) {
                    null
                } else {
                    ReminderUiState(
                        id = reminder.id,
                        title = reminder.title,
                        description = reminder.description,
                        date = reminder.date,
                        notify = reminder.notify,
                        audioRecordings = reminder.audioRecordings,
                        attachments = reminder.attachments
                    )
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
                initialValue = null // Start with null until the DB loads
            )

    fun deleteReminder() {
        viewModelScope.launch {
            uiState.value?.let { remindersRepository.delete(it.toReminder()) }
        }
    }

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}
