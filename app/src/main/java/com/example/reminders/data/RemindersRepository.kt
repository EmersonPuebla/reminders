package com.example.reminders.data

import kotlinx.coroutines.flow.Flow

interface RemindersRepository {
    fun getAllReminders(): Flow<List<Reminder>>
    fun getReminder(id: Int): Flow<Reminder?>
    suspend fun insert(reminder: Reminder)
    suspend fun update(reminder: Reminder)
    suspend fun delete(reminder: Reminder)
    suspend fun deleteReminders(ids: List<Int>)
    suspend fun syncReminders()
    suspend fun syncRemindersAndFetchMissing(): SyncResult

    // Methods for synchronization worker
    suspend fun getDeletedAndUnsynced(): List<Reminder>
    suspend fun getUnsynced(): List<Reminder>
    suspend fun markAsDeleted(id: Int, timestamp: Long)
    suspend fun deleteById(id: Int)
    suspend fun sync(): SyncResult

    // Methods for periodic sync
    fun schedulePeriodicSync(interval: Long)
    fun cancelPeriodicSync()
}

/**
 * Resultado de la sincronización con información sobre lo que se sincronizó.
 */
data class SyncResult(
    val success: Boolean,
    val message: String,
    val newRemindersCount: Int = 0,
    val updatedRemindersCount: Int = 0,
    val deletedRemindersCount: Int = 0
)

