package com.example.reminders

import android.app.Application
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.reminders.data.AppContainer
import com.example.reminders.data.AppDataContainer
import com.example.reminders.data.network.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class RemindersApplication : Application() {
    lateinit var container: AppContainer
    private val applicationScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this)
        Log.d("RemindersApp", "Aplicación inicializada")
        delayedInit()
    }

    private fun delayedInit() {
        applicationScope.launch {
            try {
                Log.d("RemindersApp", "Iniciando sincronización en startup")
                container.remindersRepository.syncReminders()
                
                val syncEnabled = container.userPreferencesRepository.syncEnabled.first()
                Log.d("RemindersApp", "Sync habilitado: $syncEnabled")
                
                if (syncEnabled) {
                    val syncInterval = container.userPreferencesRepository.syncInterval.first()
                    Log.d("RemindersApp", "Configurando trabajo periódico cada $syncInterval minutos")
                    setupRecurringWork(syncInterval.toLong())
                }
            } catch (e: Exception) {
                Log.e("RemindersApp", "Error en delayedInit: ${e.message}", e)
                e.printStackTrace()
            }
        }
    }

    private fun setupRecurringWork(syncInterval: Long) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // Asegurar intervalo mínimo de 15 minutos (WorkManager lo requiere)
            val finalInterval = if (syncInterval < 15) 15L else syncInterval

            val repeatingRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                finalInterval,
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            Log.d("RemindersApp", "Encolando trabajo periódico: sync_reminders_work (intervalo: $finalInterval min)")
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "sync_reminders_work",
                ExistingPeriodicWorkPolicy.KEEP,
                repeatingRequest
            )
            Log.d("RemindersApp", "Trabajo periódico encolado exitosamente")
        } catch (e: Exception) {
            Log.e("RemindersApp", "Error en setupRecurringWork: ${e.message}", e)
            e.printStackTrace()
        }
    }
}
