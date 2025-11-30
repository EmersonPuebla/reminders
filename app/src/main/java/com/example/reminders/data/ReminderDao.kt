package com.example.reminders.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: Reminder)

    @Update
    suspend fun update(reminder: Reminder)

    @Transaction
    suspend fun updateRemindersOrder(reminders: List<Reminder>) {
        reminders.forEach { update(it) }
    }

    @Delete
    suspend fun delete(reminder: Reminder)

    @Query("DELETE FROM reminders WHERE id IN (:ids)")
    suspend fun deleteReminders(ids: List<Int>)

    @Query("SELECT * FROM reminders WHERE id = :id")
    fun getReminder(id: Int): Flow<Reminder?>

    @Query("SELECT * FROM reminders ORDER BY sortOrder ASC, date ASC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders ORDER BY date DESC")
    suspend fun getAllRemindersSync(): List<Reminder>
}
