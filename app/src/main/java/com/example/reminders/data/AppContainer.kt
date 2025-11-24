package com.example.reminders.data

import android.content.Context
import android.util.Log
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
        ApiConnectionService(context, userPreferencesRepository)
    }

    override val apiService: ApiService
        get() = apiConnectionService.getApiService()

    override val remindersRepository: RemindersRepository by lazy {
        CashedRemindersRepository(
            reminderDao = AppDatabase.getDatabase(context).reminderDao(),
            apiService = apiService,
            userPreferencesRepository = userPreferencesRepository
        )
    }
}
