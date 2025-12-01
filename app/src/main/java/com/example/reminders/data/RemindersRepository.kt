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

class OfflineRemindersRepository(private val reminderDao: ReminderDao) : RemindersRepository {
    override fun getAllReminders(): Flow<List<Reminder>> = reminderDao.getAllReminders()
    override fun getReminder(id: Int): Flow<Reminder?> = reminderDao.getReminder(id)
    override suspend fun insert(reminder: Reminder) = reminderDao.insert(reminder)
    override suspend fun update(reminder: Reminder) = reminderDao.update(reminder)
    override suspend fun delete(reminder: Reminder) = reminderDao.delete(reminder)
    override suspend fun deleteReminders(ids: List<Int>) = reminderDao.deleteReminders(ids)
    override suspend fun syncReminders() { /* No-op for offline repository */ }
    override suspend fun syncRemindersAndFetchMissing(): SyncResult {
        return SyncResult(false, "No hay conexión de API configurada")
    }

    override suspend fun getDeletedAndUnsynced(): List<Reminder> = emptyList()
    override suspend fun getUnsynced(): List<Reminder> = emptyList()
    override suspend fun markAsDeleted(id: Int, timestamp: Long) {}
    override suspend fun deleteById(id: Int) {}
    override suspend fun sync(): SyncResult = SyncResult(true, "Offline sync complete")

    // Periodic sync is a no-op for the offline repository
    override fun schedulePeriodicSync(interval: Long) {}
    override fun cancelPeriodicSync() {}
}
