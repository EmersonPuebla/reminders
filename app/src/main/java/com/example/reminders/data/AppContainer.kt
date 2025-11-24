package com.example.reminders.data

import android.content.Context
import android.util.Log
import com.example.reminders.data.network.ApiService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

interface AppContainer {
    val remindersRepository: RemindersRepository
    val userPreferencesRepository: UserPreferencesRepository
    val apiService: ApiService
}

class AppDataContainer(private val context: Context) : AppContainer {

    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context)
    }

    override val apiService: ApiService
        get() {
            val address = runBlocking { userPreferencesRepository.serverAddress.first() }
            val port = runBlocking { userPreferencesRepository.serverPort.first() }
            val useHttps = runBlocking { userPreferencesRepository.useHttps.first() }

            val protocol = if (useHttps) "https" else "http"
            val baseUrl = if (address.isNotBlank()) {
                if (port.isNotBlank()) {
                    "$protocol://$address:$port/"
                } else {
                    "$protocol://$address/"
                }
            } else {
                "http://10.0.2.2:8080/"
            }

            Log.d("ApiService", "Conectando a: $baseUrl")

            val loggingInterceptor = HttpLoggingInterceptor { message ->
                Log.d("OkHttp", message)
            }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val httpClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(ApiService::class.java)
        }

    override val remindersRepository: RemindersRepository by lazy {
        CashedRemindersRepository(
            reminderDao = AppDatabase.getDatabase(context).reminderDao(),
            apiService = apiService,
            userPreferencesRepository = userPreferencesRepository
        )
    }
}
