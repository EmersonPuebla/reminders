package com.example.reminders.data.network

import android.content.Context
import android.util.Log
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters

class SyncWorkerFactory : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            SyncWorker::class.java.name -> {
                Log.d("SyncWorkerFactory", "Creando instancia de SyncWorker")
                SyncWorker(appContext, workerParameters)
            }
            else -> {
                Log.w("SyncWorkerFactory", "Worker desconocido: $workerClassName")
                null
            }
        }
    }
}