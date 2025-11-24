package com.example.reminders.data.network

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.reminders.RemindersApplication

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("SyncWorker", "Iniciando trabajo de sincronización...")
            
            // Obtener el container desde la aplicación
            val app = applicationContext as? RemindersApplication
            if (app == null) {
                Log.e("SyncWorker", "No se pudo obtener la aplicación")
                return Result.retry()
            }
            
            app.container.remindersRepository.syncReminders()
            Log.d("SyncWorker", "Sincronización completada exitosamente")
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Error en sincronización: ${e.message}", e)
            e.printStackTrace()
            Result.retry()
        }
    }
}
