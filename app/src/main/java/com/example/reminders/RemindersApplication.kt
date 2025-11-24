package com.example.reminders

import android.app.Application
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.reminders.data.AppContainer
import com.example.reminders.data.AppDataContainer
import com.example.reminders.data.network.SyncWorker
import com.example.reminders.data.network.SyncWorkerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class RemindersApplication : Application(), Configuration.Provider {
    lateinit var container: AppContainer
    private val applicationScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this)
        delayedInit()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(SyncWorkerFactory(container.remindersRepository))
            .build()

    private fun delayedInit() {
        applicationScope.launch {
            container.remindersRepository.syncReminders() // Sync on startup
            val syncEnabled = container.userPreferencesRepository.syncEnabled.first()
            if (syncEnabled) {
                val syncInterval = container.userPreferencesRepository.syncInterval.first()
                setupRecurringWork(syncInterval.toLong())
            }
        }
    }

    private fun setupRecurringWork(syncInterval: Long) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val repeatingRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            syncInterval,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "sync_reminders_work",
            ExistingPeriodicWorkPolicy.KEEP,
            repeatingRequest
        )
    }
}
