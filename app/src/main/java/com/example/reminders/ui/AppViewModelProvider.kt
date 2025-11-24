package com.example.reminders.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.reminders.RemindersApplication
import com.example.reminders.ui.screens.ReminderDetailViewModel
import com.example.reminders.ui.screens.ReminderListViewModel
import com.example.reminders.ui.screens.SettingsViewModel
import com.example.reminders.ui.screens.ViewReminderViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            ReminderListViewModel(
                remindersApplication().container.remindersRepository
            )
        }
        initializer {
            ReminderDetailViewModel(
                remindersRepository = remindersApplication().container.remindersRepository,
                savedStateHandle = this.createSavedStateHandle()
            )
        }
        initializer {
            ViewReminderViewModel(
                this.createSavedStateHandle(),
                remindersApplication().container.remindersRepository
            )
        }
        initializer {
            SettingsViewModel(
                userPreferencesRepository = remindersApplication().container.userPreferencesRepository,
                apiService = remindersApplication().container.apiService
            )
        }
    }
}

fun CreationExtras.remindersApplication(): RemindersApplication = 
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RemindersApplication)
