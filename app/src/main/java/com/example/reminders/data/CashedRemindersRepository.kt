package com.example.reminders.data

import com.example.reminders.data.network.ApiService
import com.example.reminders.data.network.AttachmentDto
import com.example.reminders.data.network.ReminderDto
import com.example.reminders.data.network.VoiceNoteDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class CashedRemindersRepository(
    private val reminderDao: ReminderDao,
    private val apiService: ApiService,
    private val userPreferencesRepository: UserPreferencesRepository
) : RemindersRepository {

    private val converters = Converters()

    override fun getAllReminders(): Flow<List<Reminder>> = reminderDao.getAllReminders()

    override fun getReminder(id: Int): Flow<Reminder?> = reminderDao.getReminder(id)

    override suspend fun insert(reminder: Reminder) {
        reminderDao.insert(reminder)
        if (userPreferencesRepository.syncEnabled.first()) {
            try {
                apiService.createReminder(reminder.toDto())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun update(reminder: Reminder) {
        reminderDao.update(reminder)
        if (userPreferencesRepository.syncEnabled.first()) {
            try {
                apiService.updateReminder(reminder.id, reminder.toDto())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun delete(reminder: Reminder) {
        reminderDao.delete(reminder)
        if (userPreferencesRepository.syncEnabled.first()) {
            try {
                apiService.deleteReminder(reminder.id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun syncReminders() {
        if (!userPreferencesRepository.syncEnabled.first()) return
        try {
            val response = apiService.getAllReminders()
            if (response.success) {
                response.data?.forEach { reminderDto ->
                    reminderDao.insert(reminderDto.toEntity())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun Reminder.toDto(): ReminderDto {
        return ReminderDto(
            id = this.id,
            title = this.title,
            description = this.description,
            notify = this.notify,
            notifyDate = converters.fromTimestamp(this.notifyDate),
            date = converters.fromTimestamp(this.date) ?: "",
            voiceNotes = this.audioRecordings.map { VoiceNoteDto(name = it.key, data = it.value) },
            attachments = this.attachments.map { AttachmentDto(name = it.key, data = it.value) }
        )
    }

    private fun ReminderDto.toEntity(): Reminder {
        return Reminder(
            id = this.id,
            title = this.title,
            description = this.description,
            date = converters.dateToTimestamp(this.date) ?: 0,
            notify = this.notify,
            notifyDate = converters.dateToTimestamp(this.notifyDate),
            audioRecordings = this.voiceNotes.associate { it.name to (it.data ?: "") },
            attachments = this.attachments.associate { it.name to (it.data ?: "") }
        )
    }
}
