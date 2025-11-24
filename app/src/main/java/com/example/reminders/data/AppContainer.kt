package com.example.reminders.data

import android.content.Context
import com.example.reminders.data.network.ApiService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

interface AppContainer {
    val remindersRepository: RemindersRepository
    val userPreferencesRepository: UserPreferencesRepository
}

class AppDataContainer(private val context: Context) : AppContainer {

    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context)
    }

    private val apiService: ApiService by lazy {
        val address = runBlocking { userPreferencesRepository.serverAddress.first() }
        val port = runBlocking { userPreferencesRepository.serverPort.first() }

        val baseUrl = if (address.isNotBlank() && port.isNotBlank()) {
            "http://$address:$port/"
        } else {
            "http://localhost/"
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(ApiService::class.java)
    }

    override val remindersRepository: RemindersRepository by lazy {
        CashedRemindersRepository(
            reminderDao = AppDatabase.getDatabase(context).reminderDao(),
            apiService = apiService,
            userPreferencesRepository = userPreferencesRepository
        )
    }
}
