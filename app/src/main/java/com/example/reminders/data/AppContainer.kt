package com.example.reminders.data

import android.content.Context
import androidx.work.WorkManager
import com.example.reminders.data.network.ApiConnectionService
import com.example.reminders.data.network.ApiService

interface AppContainer {
    val remindersRepository: RemindersRepository
    val userPreferencesRepository: UserPreferencesRepository
    val apiService: ApiService
}

class AppDataContainer(private val context: Context) : AppContainer {

    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context)
    }

    private val apiConnectionService: ApiConnectionService by lazy {
        ApiConnectionService(userPreferencesRepository)
    }

    override val apiService: ApiService
        get() = apiConnectionService.apiService

    private val workManager: WorkManager by lazy {
        WorkManager.getInstance(context)
    }

    override val remindersRepository: RemindersRepository by lazy {
        CashedRemindersRepository(
            reminderDao = AppDatabase.getDatabase(context).reminderDao(),
            apiService = apiService,
            userPreferencesRepository = userPreferencesRepository,
            context = context,
            workManager = workManager
        )
    }
}
