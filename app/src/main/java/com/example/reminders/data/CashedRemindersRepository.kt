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
            if (local.isDeleted) continue // Los borrados se manejan en la Fase 3

            // CORRECCIÓN: Identificar si el recordatorio es nuevo (ID=0) o ha sido modificado.
            val remote = remoteMap[local.id]
            val isNew = local.id == 0 // Asume que los nuevos elementos tienen ID 0

            if (isNew) { // El recordatorio es completamente nuevo
                try {
                    Log.d("CashedRepository", "Creando en servidor (nuevo): ${local.title}")
                    val createdDto = apiService.createReminder(toDto(local)).data
                    if (createdDto != null) {
                        // Una vez creado, borramos el local con ID 0 y guardamos el nuevo con el ID del servidor.
                        reminderDao.deleteById(local.id)
                        reminderDao.insert(toEntity(createdDto).copy(isSynced = true))
                    }
                } catch (e: Exception) {
                    Log.e("CashedRepository", "Error creando '${local.title}': ${e.message}")
                }
            } else if (remote == null) { // El recordatorio es nuevo para el servidor pero ya tiene ID local
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
        val freshLocalReminders = reminderDao.getAllRemindersSync() // Volvemos a cargar para tener los IDs actualizados
        val freshLocalMap = freshLocalReminders.associateBy { it.id }

        for (remote in remoteReminders) {
            val local = freshLocalMap[remote.id]
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
        val finalLocalReminders = reminderDao.getAllRemindersSync()
        val localIdsToDelete = finalLocalReminders.filter { it.isDeleted && it.id != 0 }.map { it.id }.toSet()

        localIdsToDelete.forEach { id ->
            try {
                Log.d("CashedRepository", "Eliminando de servidor: ID $id")
                apiService.deleteReminder(id)
                reminderDao.deleteById(id) // Eliminación definitiva del registro local
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
            id = if (reminder.id == 0) 0 else reminder.id, // Envía 0 para nuevos, o el ID existente
            title = reminder.title,
            description = reminder.description,
            notify = reminder.notify,
            notifyDate = converters.fromTimestamp(reminder.notifyDate),
            date = converters.fromTimestamp(reminder.date) ?: "",
            voiceNotes = reminder.audioRecordings.map { (filePath, name) ->
                val file = File(filePath)
                val dataToSend = if (file.exists() && file.isFile) {
                    // Si el archivo existe localmente, lo leemos y codificamos.
                    try {
                        Log.d("CashedRepository", "Codificando nota de voz local: $name")
                        Base64.encodeToString(file.readBytes(), Base64.DEFAULT)
                    } catch (e: Exception) {
                        Log.e("CashedRepository", "Error al leer el archivo de audio $filePath: ${e.message}")
                        "" // Envía vacío si hay error de lectura
                    }
                } else {
                    // Si no es un archivo local (es una URL/ID), se envía la data original (el path/URL).
                    // Esto es clave para las actualizaciones, para no perder la referencia al archivo ya subido.
                    Log.d("CashedRepository", "Reenviando referencia de nota de voz existente: $name")
                    filePath
                }
                VoiceNoteDto(name = name, data = dataToSend)
            },
            attachments = reminder.attachments.map { (filePath, name) ->
                val file = File(filePath)
                val dataToSend = if (file.exists() && file.isFile) {
                    // Si el archivo existe localmente, lo leemos y codificamos.
                    try {
                        Log.d("CashedRepository", "Codificando adjunto local: $name")
                        Base64.encodeToString(file.readBytes(), Base64.DEFAULT)
                    } catch (e: Exception) {
                        Log.e("CashedRepository", "Error al leer el adjunto $filePath: ${e.message}")
                        "" // Envía vacío si hay error de lectura
                    }
                } else {
                    // Reenvía la referencia del adjunto ya existente en el servidor.
                    Log.d("CashedRepository", "Reenviando referencia de adjunto existente: $name")
                    filePath
                }
                AttachmentDto(name = name, data = dataToSend)
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