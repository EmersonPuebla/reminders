package com.example.reminders.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.reminders.RemindersApplication

class SyncReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Obtiene el repositorio desde el contenedor de dependencias de la aplicación
        val repository = (applicationContext as RemindersApplication).container.remindersRepository
        
        return try {
            Log.d("SyncReminderWorker", "Iniciando tarea de sincronización.")
            // Llama al método centralizado de sincronización en el repositorio
            repository.sync()
            Log.d("SyncReminderWorker", "Tarea de sincronización completada exitosamente.")
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncReminderWorker", "Error en la tarea de sincronización: ${e.message}")
            // Reintentar el trabajo si falla
            Result.retry()
        }
    }
}
