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

class EditReminderViewModel (
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
        if (reminderId != null && reminderId != 0) { // Un ID de 0 indica un nuevo recordatorio
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
        reminderUiState = newReminderUiState.copy(lastModified = System.currentTimeMillis())
    }

    suspend fun saveReminder() {
        val reminderToSave = reminderUiState.toReminder()
        if (reminderToSave.id == 0) {
            remindersRepository.insert(reminderToSave)
        } else {
            remindersRepository.update(reminderToSave)
        }
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
    val attachments: Map<String, String> = emptyMap(),
    val lastModified: Long = 0L
)

fun ReminderUiState.toReminder(): Reminder = Reminder(
    id = id,
    title = title,
    description = description,
    date = date,
    notify = notify,
    notifyDate = notifyDate,
    audioRecordings = audioRecordings,
    attachments = attachments,
    lastModified = lastModified
)

fun Reminder.toReminderUiState(): ReminderUiState = ReminderUiState(
    id = id,
    title = title,
    description = description,
    date = date,
    notify = notify,
    notifyDate = notifyDate ?: 0L,
    audioRecordings = audioRecordings,
    attachments = attachments,
    lastModified = lastModified
)
