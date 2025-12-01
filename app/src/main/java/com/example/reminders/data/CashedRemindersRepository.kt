package com.example.reminders.data

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.reminders.data.network.ApiService
import com.example.reminders.data.network.AttachmentDto
import com.example.reminders.data.network.ReminderDto
import com.example.reminders.data.network.VoiceNoteDto
import com.example.reminders.data.worker.SyncReminderWorker
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.concurrent.TimeUnit

class CashedRemindersRepository(
    private val reminderDao: ReminderDao,
    private val apiService: ApiService,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val context: Context,
    private val workManager: WorkManager
) : RemindersRepository {

    private val converters = Converters()
    private val periodicSyncTag = "periodic_sync_work"

    override fun getAllReminders(): Flow<List<Reminder>> = reminderDao.getAllReminders()

    override fun getReminder(id: Int): Flow<Reminder?> = reminderDao.getReminder(id)

    override suspend fun insert(reminder: Reminder) {
        // Actualiza lastModified y marca como no sincronizado
        reminderDao.insert(reminder.copy(isSynced = false, lastModified = System.currentTimeMillis()))
        scheduleSync()
    }

    override suspend fun update(reminder: Reminder) {
        // Actualiza lastModified y marca como no sincronizado
        reminderDao.update(reminder.copy(isSynced = false, lastModified = System.currentTimeMillis()))
        scheduleSync()
    }

    override suspend fun delete(reminder: Reminder) {
        // Realiza un borrado suave (soft delete) actualizando la marca de tiempo
        markAsDeleted(reminder.id, System.currentTimeMillis())
        scheduleSync()
    }

    override suspend fun deleteReminders(ids: List<Int>) {
        ids.forEach { markAsDeleted(it, System.currentTimeMillis()) }
        scheduleSync()
    }

    private fun scheduleSync() {
        val workRequest = OneTimeWorkRequestBuilder<SyncReminderWorker>().build()
        workManager.enqueue(workRequest)
    }

    override fun schedulePeriodicSync(interval: Long) {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val repeatingRequest = PeriodicWorkRequestBuilder<SyncReminderWorker>(interval, TimeUnit.MINUTES)
            .setConstraints(constraints).addTag(periodicSyncTag).build()
        workManager.enqueueUniquePeriodicWork(periodicSyncTag, ExistingPeriodicWorkPolicy.REPLACE, repeatingRequest)
    }

    override fun cancelPeriodicSync() {
        workManager.cancelAllWorkByTag(periodicSyncTag)
    }

    override suspend fun sync(): SyncResult {
        Log.d("CashedRepository", "Iniciando sincronización 'Git Style'...")

        val localReminders = reminderDao.getAllRemindersSync()
        val remoteReminders = try {
            apiService.getAllReminders().data ?: emptyList()
        } catch (e: Exception) {
            Log.e("CashedRepository", "Error de conexión: ${e.message}")
            return SyncResult(success = false, message = "Error de conexión con el servidor.")
        }

        val localMap = localReminders.associateBy { it.id }
        val remoteMap = remoteReminders.associateBy { it.id }

        // Fase 1: "Push" - Subir cambios locales a la nube
        Log.d("CashedRepository", "Fase 1: Subiendo cambios locales...")
        for (local in localReminders) {
            val remote = remoteMap[local.id]
            if (local.isDeleted) continue // Los borrados se manejan en la Fase 3

            if (remote == null) { // El recordatorio es nuevo para el servidor
                try {
                    Log.d("CashedRepository", "Creando en servidor: ${local.title}")
                    apiService.createReminder(toDto(local))
                    reminderDao.update(local.copy(isSynced = true))
                } catch (e: Exception) {
                    Log.e("CashedRepository", "Error creando '${local.title}': ${e.message}")
                }
            } else if (local.lastModified > remote.lastModified) { // El local es más reciente
                try {
                    Log.d("CashedRepository", "Actualizando en servidor: ${local.title}")
                    apiService.updateReminder(local.id, toDto(local))
                    reminderDao.update(local.copy(isSynced = true))
                } catch (e: Exception) {
                    Log.e("CashedRepository", "Error actualizando '${local.title}': ${e.message}")
                }
            }
        }

        // Fase 2: "Pull" - Bajar cambios de la nube
        Log.d("CashedRepository", "Fase 2: Descargando cambios del servidor...")
        for (remote in remoteReminders) {
            val local = localMap[remote.id]
            if (local == null) { // Es nuevo para el dispositivo local
                Log.d("CashedRepository", "Descargando nuevo: ${remote.title}")
                reminderDao.insert(toEntity(remote).copy(isSynced = true))
            } else if (remote.lastModified > local.lastModified) { // El remoto es más reciente
                Log.d("CashedRepository", "Actualizando desde servidor: ${remote.title}")
                reminderDao.update(toEntity(remote).copy(isSynced = true))
            }
        }

        // Fase 3: "Limpieza" - Sincronizar eliminaciones
        Log.d("CashedRepository", "Fase 3: Sincronizando eliminaciones...")
        val serverIds = remoteMap.keys
        val localIds = localMap.values.filter { !it.isDeleted }.map { it.id }.toSet()
        val idsToDelete = serverIds - localIds

        idsToDelete.forEach { id ->
            try {
                Log.d("CashedRepository", "Eliminando de servidor: ID $id")
                apiService.deleteReminder(id)
            } catch (e: Exception) {
                Log.e("CashedRepository", "Error eliminando ID $id: ${e.message}")
            }
        }

        Log.i("CashedRepository", "Sincronización completada.")
        return SyncResult(success = true, message = "Sincronización completada exitosamente.")
    }

    override suspend fun getDeletedAndUnsynced(): List<Reminder> = reminderDao.getDeletedAndUnsynced()
    override suspend fun getUnsynced(): List<Reminder> = reminderDao.getUnsynced()
    override suspend fun markAsDeleted(id: Int, timestamp: Long) = reminderDao.markAsDeleted(id, timestamp)
    override suspend fun deleteById(id: Int) = reminderDao.deleteById(id)
    
    override suspend fun syncReminders() {
        sync()
    }

    override suspend fun syncRemindersAndFetchMissing(): SyncResult = sync()

    private fun toDto(reminder: Reminder): ReminderDto {
        return ReminderDto(
            id = reminder.id,
            title = reminder.title,
            description = reminder.description,
            notify = reminder.notify,
            notifyDate = converters.fromTimestamp(reminder.notifyDate),
            date = converters.fromTimestamp(reminder.date) ?: "",
            voiceNotes = reminder.audioRecordings.map { (filePath, name) ->
                VoiceNoteDto(name = name, data = if (filePath.isNotEmpty()) {
                    try {
                        val file = File(filePath)
                        if (file.exists()) Base64.encodeToString(file.readBytes(), Base64.DEFAULT) else filePath
                    } catch (e: Exception) {
                        filePath
                    }
                } else "")
            },
            attachments = reminder.attachments.map { (filePath, name) ->
                AttachmentDto(name = name, data = if (filePath.isNotEmpty()) {
                    try {
                        val file = File(filePath)
                        if (file.exists()) Base64.encodeToString(file.readBytes(), Base64.DEFAULT) else filePath
                    } catch (e: Exception) {
                        filePath
                    }
                } else "")
            },
            lastModified = reminder.lastModified
        )
    }

    private fun toEntity(dto: ReminderDto): Reminder {
        val audioDir = File(context.filesDir, "audio_recordings").apply { mkdirs() }
        val attachmentsDir = File(context.filesDir, "attachments").apply { mkdirs() }

        return Reminder(
            id = dto.id,
            title = dto.title,
            description = dto.description,
            date = converters.dateToTimestamp(dto.date) ?: 0,
            notify = dto.notify,
            notifyDate = converters.dateToTimestamp(dto.notifyDate),
            audioRecordings = (dto.voiceNotes ?: emptyList()).associate {
                val filePath = try {
                    val file = File(audioDir, "${System.currentTimeMillis()}_${it.name}")
                    file.writeBytes(Base64.decode(it.data, Base64.DEFAULT))
                    file.absolutePath
                } catch (e: Exception) { it.data ?: "" }
                it.name to filePath
            },
            attachments = (dto.attachments ?: emptyList()).associate {
                val filePath = try {
                    val file = File(attachmentsDir, "${System.currentTimeMillis()}_${it.name}")
                    file.writeBytes(Base64.decode(it.data, Base64.DEFAULT))
                    file.absolutePath
                } catch (e: Exception) { it.data ?: "" }
                it.name to filePath
            },
            lastModified = dto.lastModified
        )
    }
}