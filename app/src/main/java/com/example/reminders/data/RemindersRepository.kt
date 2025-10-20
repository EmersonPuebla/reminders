package com.example.reminders.data

import kotlinx.coroutines.flow.Flow

interface RemindersRepository {
    fun getAllReminders(): Flow<List<Reminder>>
    fun getReminder(id: Int): Flow<Reminder?>
    suspend fun insert(reminder: Reminder)
    suspend fun delete(reminder: Reminder)
}

class OfflineRemindersRepository(private val reminderDao: ReminderDao) : RemindersRepository {
    override fun getAllReminders(): Flow<List<Reminder>> = reminderDao.getAllReminders()
    override fun getReminder(id: Int): Flow<Reminder?> = reminderDao.getReminder(id)
    override suspend fun insert(reminder: Reminder) = reminderDao.insert(reminder)
    override suspend fun delete(reminder: Reminder) = reminderDao.delete(reminder)
}
