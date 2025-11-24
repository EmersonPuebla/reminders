package com.example.reminders.data

import android.content.Context
import android.util.Base64
import com.example.reminders.data.network.ApiService
import com.example.reminders.data.network.AttachmentDto
import com.example.reminders.data.network.ReminderDto
import com.example.reminders.data.network.VoiceNoteDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

class CashedRemindersRepository(
    private val reminderDao: ReminderDao,
    private val apiService: ApiService,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val context: Context
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

    override suspend fun syncRemindersAndFetchMissing(): SyncResult {
        if (!userPreferencesRepository.syncEnabled.first()) {
            android.util.Log.d("CashedRepository", "Sync deshabilitado - syncRemindersAndFetchMissing abortado")
            return SyncResult(false, "Sincronización deshabilitada")
        }

        return try {
            android.util.Log.d("CashedRepository", "Iniciando sincronización (Cliente es la fuente de verdad)")
            
            // 1. Obtener recordatorios locales
            val localReminders = reminderDao.getAllRemindersSync()
            android.util.Log.d("CashedRepository", "Recordatorios locales: ${localReminders.size}")
            
            // 2. Obtener recordatorios de la API
            val apiResponse = apiService.getAllReminders()
            if (!apiResponse.success || apiResponse.data == null) {
                return SyncResult(
                    success = false,
                    message = "Error al obtener recordatorios de la API: ${apiResponse.message}"
                )
            }
            
            val remindersFromApi = apiResponse.data
            android.util.Log.d("CashedRepository", "Recordatorios en API: ${remindersFromApi.size}")

            var syncedCount = 0
            var newRemindersCount = 0

            // 3. Crear mapas para comparación rápida
            val apiMap = remindersFromApi.associateBy { it.id }
            val apiTitleMap = remindersFromApi.associateBy { it.title }

            // 4. PASO 1: Enviar estado actual del cliente a la API
            // El cliente siempre prevalece. Sincronizar todos los recordatorios locales a la API
            for (localReminder in localReminders) {
                try {
                    if (apiMap.containsKey(localReminder.id)) {
                        // Ya existe en API - actualizar para que coincida exactamente con local
                        val result = apiService.updateReminder(localReminder.id, localReminder.toDto())
                        if (result.success) {
                            android.util.Log.d("CashedRepository", "Recordatorio sincronizado (update): ${localReminder.title}")
                            syncedCount++
                        }
                    } else {
                        // No existe en API - crear
                        val result = apiService.createReminder(localReminder.toDto())
                        if (result.success) {
                            android.util.Log.d("CashedRepository", "Recordatorio sincronizado (create): ${localReminder.title}")
                            syncedCount++
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CashedRepository", "Error al sincronizar ${localReminder.title}: ${e.message}")
                }
            }

            // 5. PASO 2: Eliminar de API los registros que ya no existen localmente
            val localTitles = localReminders.map { it.title }.toSet()
            for (apiReminder in remindersFromApi) {
                if (!localTitles.contains(apiReminder.title)) {
                    try {
                        val result = apiService.deleteReminder(apiReminder.id)
                        if (result.success) {
                            android.util.Log.d("CashedRepository", "Recordatorio eliminado de API (no existe en local): ${apiReminder.title}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("CashedRepository", "Error al eliminar ${apiReminder.title} de API: ${e.message}")
                    }
                }
            }

            // 6. PASO 3: Obtener recordatorios faltantes de la API (que existan en API pero no en local)
            // Esto solo ocurriría si se agregaron desde otra fuente
            val finalApiResponse = apiService.getAllReminders()
            val localIds = localReminders.map { it.id }.toSet()
            
            if (finalApiResponse.success && finalApiResponse.data != null) {
                for (reminderDto in finalApiResponse.data) {
                    // Si existe en API pero no en local, descargar
                    if (!localIds.contains(reminderDto.id)) {
                        try {
                            reminderDao.insert(reminderDto.toEntity())
                            newRemindersCount++
                            android.util.Log.d("CashedRepository", "Nuevo recordatorio descargado desde API: ${reminderDto.title}")
                        } catch (e: Exception) {
                            android.util.Log.e("CashedRepository", "Error al descargar ${reminderDto.title}: ${e.message}")
                        }
                    }
                }
            }

            android.util.Log.d(
                "CashedRepository",
                "Sincronización completada - Sincronizados: $syncedCount, Nuevos descargados: $newRemindersCount"
            )

            SyncResult(
                success = true,
                message = "Sincronización completada: $syncedCount sincronizados, $newRemindersCount nuevos descargados",
                newRemindersCount = newRemindersCount,
                updatedRemindersCount = syncedCount,
                deletedRemindersCount = remindersFromApi.size - syncedCount
            )
        } catch (e: Exception) {
            android.util.Log.e("CashedRepository", "Excepción en syncRemindersAndFetchMissing: ${e.message}", e)
            e.printStackTrace()
            SyncResult(false, "Error durante la sincronización: ${e.message}")
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
            voiceNotes = this.audioRecordings.map { (filePath, name) ->
                // Convertir archivo de audio a base64
                val base64Data = if (filePath.isNotEmpty()) {
                    try {
                        val file = File(filePath)
                        if (file.exists()) {
                            val fileBytes = file.readBytes()
                            Base64.encodeToString(fileBytes, Base64.DEFAULT)
                        } else {
                            filePath // Si no existe el archivo, enviar la ruta como estaba
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("CashedRepository", "Error al codificar audio $name: ${e.message}")
                        filePath
                    }
                } else {
                    ""
                }
                VoiceNoteDto(name = name, data = base64Data)
            },
            attachments = this.attachments.map { (filePath, name) ->
                // Convertir archivo adjunto a base64
                val base64Data = if (filePath.isNotEmpty()) {
                    try {
                        val file = File(filePath)
                        if (file.exists()) {
                            val fileBytes = file.readBytes()
                            Base64.encodeToString(fileBytes, Base64.DEFAULT)
                        } else {
                            filePath // Si no existe el archivo, enviar la ruta como estaba
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("CashedRepository", "Error al codificar archivo $name: ${e.message}")
                        filePath
                    }
                } else {
                    ""
                }
                AttachmentDto(name = name, data = base64Data)
            }
        )
    }

    private fun ReminderDto.toEntity(): Reminder {
        // Crear directorios si no existen
        val audioDir = File(context.filesDir, "audio_recordings")
        val attachmentsDir = File(context.filesDir, "attachments")
        audioDir.mkdirs()
        attachmentsDir.mkdirs()

        return Reminder(
            id = this.id,
            title = this.title,
            description = this.description,
            date = converters.dateToTimestamp(this.date) ?: 0,
            notify = this.notify,
            notifyDate = converters.dateToTimestamp(this.notifyDate),
            audioRecordings = (this.voiceNotes ?: emptyList()).associate { voiceNote ->
                val data = voiceNote.data ?: ""
                val filename = voiceNote.name
                
                // Detectar si es base64 (contiene caracteres de base64 o es muy largo)
                val filePath = if (data.isNotEmpty() && (data.contains(Regex("[+/=]")) || data.length > 200)) {
                    try {
                        // Es base64, decodificar y guardar como archivo
                        val decodedBytes = Base64.decode(data, Base64.DEFAULT)
                        val file = File(audioDir, "${System.currentTimeMillis()}_$filename")
                        file.writeBytes(decodedBytes)
                        android.util.Log.d("CashedRepository", "Audio decodificado y guardado: ${file.absolutePath}")
                        file.absolutePath
                    } catch (e: Exception) {
                        android.util.Log.e("CashedRepository", "Error al decodificar audio $filename: ${e.message}")
                        data // Devolver como estaba si no se puede decodificar
                    }
                } else {
                    data // Ya es una ruta de archivo
                }
                
                filename to filePath
            },
            attachments = (this.attachments ?: emptyList()).associate { attachment ->
                val data = attachment.data ?: ""
                val filename = attachment.name
                
                // Detectar si es base64
                val filePath = if (data.isNotEmpty() && (data.contains(Regex("[+/=]")) || data.length > 200)) {
                    try {
                        // Es base64, decodificar y guardar como archivo
                        val decodedBytes = Base64.decode(data, Base64.DEFAULT)
                        val file = File(attachmentsDir, "${System.currentTimeMillis()}_$filename")
                        file.writeBytes(decodedBytes)
                        android.util.Log.d("CashedRepository", "Archivo decodificado y guardado: ${file.absolutePath}")
                        file.absolutePath
                    } catch (e: Exception) {
                        android.util.Log.e("CashedRepository", "Error al decodificar archivo $filename: ${e.message}")
                        data // Devolver como estaba si no se puede decodificar
                    }
                } else {
                    data // Ya es una ruta de archivo
                }
                
                filename to filePath
            }
        )
    }
}
