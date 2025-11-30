package com.example.reminders.ui.screens

import android.content.Context
import android.net.Uri
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
import java.io.File

class ReminderDetailViewModel(
    private val remindersRepository: RemindersRepository,
    private val savedStateHandle: SavedStateHandle,
    private val context: Context
) : ViewModel() {

    var reminderUiState by mutableStateOf(ReminderUiState())
        private set

    private var initialUiState = ReminderUiState()

    val hasUnsavedChanges: Boolean
        get() = reminderUiState != initialUiState

    private val reminderId: Int? = savedStateHandle["reminderId"]

    init {
        if (reminderId != null) {
            viewModelScope.launch {
                val loadedState = remindersRepository.getReminder(reminderId)
                    .filterNotNull()
                    .first()
                    .toReminderUiState()
                reminderUiState = loadedState
                initialUiState = loadedState
            }
        } else {
            initialUiState = ReminderUiState()
        }
    }

    fun updateUiState(newReminderUiState: ReminderUiState) {
        reminderUiState = newReminderUiState
    }

    suspend fun saveReminder() {
        remindersRepository.insert(reminderUiState.toReminder())
    }

    fun copyFileToInternalStorage(uri: Uri, directory: String, fileName: String): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val newFile = File(context.filesDir, "$directory/$fileName")
            newFile.parentFile?.mkdirs()
            val outputStream = newFile.outputStream()
            inputStream?.copyTo(outputStream)
            newFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

data class ReminderUiState(
    val id: Int = 0,
    val title: String = "",
    val description: String = "",
    val date: Long = 0L,
    val notify: Boolean = false,
    val notifyDate: Long = 0L,
    val audioRecordings: Map<String, String> = emptyMap(),
    val attachments: Map<String, String> = emptyMap()
)

fun ReminderUiState.toReminder(): Reminder = Reminder(
    id = id,
    title = title,
    description = description,
    date = date,
    notify = notify,
    notifyDate = notifyDate,
    audioRecordings = audioRecordings,
    attachments = attachments
)

fun Reminder.toReminderUiState(): ReminderUiState = ReminderUiState(
    id = id,
    title = title,
    description = description,
    date = date,
    notify = notify,
    notifyDate = notifyDate ?: 0L,
    audioRecordings = audioRecordings,
    attachments = attachments
)
