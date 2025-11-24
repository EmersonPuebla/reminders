package com.example.reminders.data

import com.example.reminders.data.network.ApiService
import com.example.reminders.data.network.AttachmentDto
import com.example.reminders.data.network.ReminderDto
import com.example.reminders.data.network.VoiceNoteDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CashedRemindersRepository(
    private val reminderDao: ReminderDao,
    private val apiService: ApiService,
    private val userPreferencesRepository: UserPreferencesRepository
) : RemindersRepository {

    private val converters = Converters()
    
    // Scope para operaciones de API que no deben ser canceladas por navegación
    private val apiScope = CoroutineScope(Dispatchers.IO)

    override fun getAllReminders(): Flow<List<Reminder>> = reminderDao.getAllReminders()

    override fun getReminder(id: Int): Flow<Reminder?> = reminderDao.getReminder(id)

    override suspend fun insert(reminder: Reminder) {
        reminderDao.insert(reminder)
        val syncEnabled = userPreferencesRepository.syncEnabled.first()
        android.util.Log.d("CashedRepository", "Insert - Sync habilitado: $syncEnabled")
        if (syncEnabled) {
            // Lanzar en scope que no será cancelado por navegación
            apiScope.launch {
                try {
                    android.util.Log.d("CashedRepository", "Insertando recordatorio en API: ${reminder.title}")
                    val result = apiService.createReminder(reminder.toDto())
                    android.util.Log.d("CashedRepository", "Respuesta de API insert: success=${result.success}")
                } catch (e: Exception) {
                    android.util.Log.e("CashedRepository", "Error al insertar en API: ${e.message}", e)
                    e.printStackTrace()
                }
            }
        } else {
            android.util.Log.d("CashedRepository", "Sync deshabilitado - registro guardado solo localmente")
        }
    }

    override suspend fun update(reminder: Reminder) {
        reminderDao.update(reminder)
        val syncEnabled = userPreferencesRepository.syncEnabled.first()
        android.util.Log.d("CashedRepository", "Update - Sync habilitado: $syncEnabled")
        if (syncEnabled) {
            // Lanzar en scope que no será cancelado por navegación
            apiScope.launch {
                try {
                    android.util.Log.d("CashedRepository", "Actualizando recordatorio en API: ${reminder.title}")
                    val result = apiService.updateReminder(reminder.id, reminder.toDto())
                    android.util.Log.d("CashedRepository", "Respuesta de API update: success=${result.success}")
                } catch (e: Exception) {
                    android.util.Log.e("CashedRepository", "Error al actualizar en API: ${e.message}", e)
                    e.printStackTrace()
                }
            }
        } else {
            android.util.Log.d("CashedRepository", "Sync deshabilitado - cambios guardados solo localmente")
        }
    }

    override suspend fun delete(reminder: Reminder) {
        reminderDao.delete(reminder)
        val syncEnabled = userPreferencesRepository.syncEnabled.first()
        android.util.Log.d("CashedRepository", "Delete - Sync habilitado: $syncEnabled")
        if (syncEnabled) {
            // Lanzar en scope que no será cancelado por navegación
            apiScope.launch {
                try {
                    android.util.Log.d("CashedRepository", "Eliminando recordatorio en API: ${reminder.title}")
                    val result = apiService.deleteReminder(reminder.id)
                    android.util.Log.d("CashedRepository", "Respuesta de API delete: success=${result.success}")
                } catch (e: Exception) {
                    android.util.Log.e("CashedRepository", "Error al eliminar en API: ${e.message}", e)
                    e.printStackTrace()
                }
            }
        } else {
            android.util.Log.d("CashedRepository", "Sync deshabilitado - eliminado solo localmente")
        }
    }

    override suspend fun deleteReminders(ids: List<Int>) {
        reminderDao.deleteReminders(ids)
        val syncEnabled = userPreferencesRepository.syncEnabled.first()
        android.util.Log.d("CashedRepository", "DeleteReminders (lote) - Sync habilitado: $syncEnabled")
        if (syncEnabled) {
            // Lanzar en scope que no será cancelado por navegación
            apiScope.launch {
                try {
                    android.util.Log.d("CashedRepository", "Eliminando ${ids.size} recordatorios en API")
                    ids.forEach { id ->
                        try {
                            apiService.deleteReminder(id)
                            android.util.Log.d("CashedRepository", "Recordatorio $id eliminado de API")
                        } catch (e: Exception) {
                            android.util.Log.e("CashedRepository", "Error al eliminar $id en API: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CashedRepository", "Error en deleteReminders: ${e.message}", e)
                    e.printStackTrace()
                }
            }
        } else {
            android.util.Log.d("CashedRepository", "Sync deshabilitado - eliminados solo localmente")
        }
    }

    override suspend fun syncReminders() {
        if (!userPreferencesRepository.syncEnabled.first()) {
            android.util.Log.d("CashedRepository", "Sync deshabilitado")
            return
        }
        try {
            android.util.Log.d("CashedRepository", "Iniciando sincronización de recordatorios")
            val response = apiService.getAllReminders()
            if (response.success) {
                android.util.Log.d("CashedRepository", "Sincronización exitosa: ${response.data?.size} recordatorios")
                response.data?.forEach { reminderDto ->
                    reminderDao.insert(reminderDto.toEntity())
                }
            } else {
                android.util.Log.e("CashedRepository", "Error en sincronización: ${response.message}")
            }
        } catch (e: Exception) {
            android.util.Log.e("CashedRepository", "Excepción en sync: ${e.message}", e)
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
