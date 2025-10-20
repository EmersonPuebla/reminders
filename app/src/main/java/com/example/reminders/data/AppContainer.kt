package com.example.reminders.data

import android.content.Context

interface AppContainer {
    val remindersRepository: RemindersRepository
}

class AppDataContainer(private val context: Context) : AppContainer {
    override val remindersRepository: RemindersRepository by lazy {
        OfflineRemindersRepository(AppDatabase.getDatabase(context).reminderDao())
    }
}
