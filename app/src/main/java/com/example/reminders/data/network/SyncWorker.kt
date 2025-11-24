package com.example.reminders.data.network

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.reminders.data.RemindersRepository

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val remindersRepository: RemindersRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            remindersRepository.syncReminders()
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
