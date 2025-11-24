package com.example.reminders.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reminders.ui.AppViewModelProvider
import com.example.reminders.utils.AudioRecorderHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDetailScreen(
    onBack: () -> Unit,
    viewModel: ReminderDetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val coroutineScope = rememberCoroutineScope()
    val uiState = viewModel.reminderUiState
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var recordingToDelete by remember { mutableStateOf<String?>(null) }
    var recordingToRename by remember { mutableStateOf<Pair<String, String>?>(null) }
    var attachmentToDelete by remember { mutableStateOf<String?>(null) }
    var attachmentToRename by remember { mutableStateOf<Pair<String, String>?>(null) }
    var recordingTimeSeconds by remember { mutableIntStateOf(0) }
    var showExitConfirmationDialog by remember { mutableStateOf(false) }
    var showNameAudioDialog by remember { mutableStateOf<String?>(null) }
    var showNameAttachmentDialog by remember { mutableStateOf<Pair<Uri, String>?>(null) }

    val audioRecorderHelper = remember { AudioRecorderHelper(context) }

    fun handleBackNavigation() {
        if (viewModel.hasUnsavedChanges) {
            showExitConfirmationDialog = true
        } else {
            onBack()
        }
    }

    BackHandler(onBack = ::handleBackNavigation)

    if (showExitConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmationDialog = false },
            title = { Text("Descartar cambios") },
            text = { Text("Si sales, los datos no guardados se perderán.") },
            confirmButton = { TextButton(onClick = onBack) { Text("Descartar") } },
            dismissButton = { TextButton(onClick = { showExitConfirmationDialog = false }) { Text("Seguir editando") } }
        )
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = getFileName(context, it) ?: "Archivo"
            showNameAttachmentDialog = it to fileName
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            isRecording = true
            audioRecorderHelper.startRecording()
        } else {
            Toast.makeText(context, "El permiso de audio es necesario", Toast.LENGTH_SHORT).show()
        }
    }

    fun Int.toMMSS(): String {
        val minutes = this / 60
        val seconds = this % 60
        return String.format(Locale.getDefault(), "%02d%02d", minutes, seconds)
    }

    @Composable
    fun RecordingTimer(isRecording: Boolean, onTimeUpdate: (Int) -> Unit) {
        LaunchedEffect(isRecording) {
            if (isRecording) {
                var time = 0
                while (isActive) {
                    delay(1000)
                    time++
                    onTimeUpdate(time)
                }
            }
        }
    }

    RecordingTimer(isRecording = isRecording) { newTime -> recordingTimeSeconds = newTime }

    HandleDialogs(
        uiState = uiState,
        viewModel = viewModel,
        recordingToDelete = recordingToDelete,
        onDismissRecordingDelete = { recordingToDelete = null },
        showNameAudioDialog = showNameAudioDialog,
        onDismissNameAudio = { showNameAudioDialog = null },
        recordingToRename = recordingToRename,
        onDismissRecordingRename = { recordingToRename = null },
        attachmentToDelete = attachmentToDelete,
        onDismissAttachmentDelete = { attachmentToDelete = null },
        showNameAttachmentDialog = showNameAttachmentDialog,
        onDismissNameAttachment = { showNameAttachmentDialog = null },
        attachmentToRename = attachmentToRename,
        onDismissAttachmentRename = { attachmentToRename = null }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.id != 0) "Editar Recordatorio" else "Crear Recordatorio") },
                navigationIcon = { IconButton(onClick = ::handleBackNavigation) { Icon(Icons.Filled.Close, "Cerrar") } },
                actions = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch { viewModel.saveReminder() }
                            onBack()
                        },
                        enabled = uiState.title.isNotBlank() && uiState.date != 0L
                    ) {
                        Text("Guardar")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), containerColor = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.size(64.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Adjuntar archivo")
                    }

                    Button(
                        onClick = {
                            if (isRecording) {
                                isRecording = false
                                audioRecorderHelper.stopRecording()?.let { showNameAudioDialog = it }
                                recordingTimeSeconds = 0
                            } else {
                                recordingTimeSeconds = 0
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier.size(64.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    ) {
                        if (isRecording) {
                            Text(text = recordingTimeSeconds.toMMSS(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onError)
                        } else {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Grabar audio")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            OutlinedTextField(value = uiState.title, onValueChange = { viewModel.updateUiState(uiState.copy(title = it)) }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(value = uiState.description, onValueChange = { viewModel.updateUiState(uiState.copy(description = it)) }, label = { Text("Detalles") }, modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 120.dp))
            Spacer(modifier = Modifier.height(24.dp))

            val showDatePicker = remember { mutableStateOf(false) }
            Button(onClick = { showDatePicker.value = true }) {
                Text(text = if (uiState.date != 0L) SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(uiState.date)) else "Seleccionar fecha")
            }

            if (showDatePicker.value) {
                val datePickerState = rememberDatePickerState()
                DatePickerDialog(
                    onDismissRequest = { showDatePicker.value = false },
                    confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { viewModel.updateUiState(uiState.copy(date = it)) }; showDatePicker.value = false }) { Text("Aceptar") } },
                    dismissButton = { TextButton(onClick = { showDatePicker.value = false }) { Text("Cancelar") } }
                ) { DatePicker(state = datePickerState) }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Notificarme")
                Spacer(modifier = Modifier.width(8.dp))
                Switch(checked = uiState.notify, onCheckedChange = { viewModel.updateUiState(uiState.copy(notify = it)) })
            }

            if (uiState.notify) {
                var showNotifyDatePicker by remember { mutableStateOf(false) }
                var showNotifyTimePicker by remember { mutableStateOf(false) }
                var selectedDate by remember { mutableStateOf<Long?>(null) }

                Button(onClick = { showNotifyDatePicker = true }) {
                    Text(text = if (uiState.notifyDate != 0L) SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(uiState.notifyDate)) else "Seleccionar fecha de notificación")
                }

                if (showNotifyDatePicker) {
                    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = if (uiState.notifyDate != 0L) uiState.notifyDate else System.currentTimeMillis())
                    DatePickerDialog(
                        onDismissRequest = { showNotifyDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                selectedDate = datePickerState.selectedDateMillis
                                showNotifyDatePicker = false
                                if (selectedDate != null) {
                                    showNotifyTimePicker = true
                                }
                            }) { Text("Aceptar") }
                        },
                        dismissButton = { TextButton(onClick = { showNotifyDatePicker = false }) { Text("Cancelar") } }
                    ) { DatePicker(state = datePickerState) }
                }

                if (showNotifyTimePicker) {
                    val calendar = Calendar.getInstance()
                    if (uiState.notifyDate != 0L) {
                        calendar.timeInMillis = uiState.notifyDate
                    }
                    val timePickerState = rememberTimePickerState(
                        initialHour = calendar.get(Calendar.HOUR_OF_DAY),
                        initialMinute = calendar.get(Calendar.MINUTE),
                        is24Hour = true
                    )
                    AlertDialog(
                        onDismissRequest = { showNotifyTimePicker = false },
                        title = { Text("Seleccionar hora") },
                        text = {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                TimePicker(state = timePickerState)
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val newCal = Calendar.getInstance().apply {
                                        timeInMillis = selectedDate!!
                                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                        set(Calendar.MINUTE, timePickerState.minute)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    viewModel.updateUiState(uiState.copy(notifyDate = newCal.timeInMillis))
                                    showNotifyTimePicker = false
                                }
                            ) { Text("Aceptar") }
                        },
                        dismissButton = {
                             TextButton(onClick = { showNotifyTimePicker = false }) { Text("Cancelar") }
                        }
                    )
                }
            }


            if (uiState.audioRecordings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Grabaciones de audio", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Column { uiState.audioRecordings.forEach { (path, name) -> AudioPlayer(audioPath = path, audioName = name, audioRecorderHelper = audioRecorderHelper, onDeleteClick = { recordingToDelete = path }, onEditClick = { recordingToRename = path to name }) } }
            }

            if (uiState.attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Archivos adjuntos", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Column { uiState.attachments.forEach { (path, name) -> AttachmentItem(attachmentName = name, onViewClick = {
                    val file = File(path)
                    val authority = "${context.packageName}.fileprovider"
                    val uri = FileProvider.getUriForFile(context, authority, file)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, context.contentResolver.getType(uri))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        Toast.makeText(context, "No se encontró una aplicación para abrir este archivo.", Toast.LENGTH_SHORT).show()
                    }
                }, onDeleteClick = { attachmentToDelete = path }, onEditClick = { attachmentToRename = path to name }) } }
            }
        }
    }

    DisposableEffect(Unit) { onDispose { audioRecorderHelper.stopPlaying() } }
}

@Composable
fun HandleDialogs(
    uiState: ReminderUiState, viewModel: ReminderDetailViewModel, recordingToDelete: String?, onDismissRecordingDelete: () -> Unit, showNameAudioDialog: String?, onDismissNameAudio: () -> Unit, recordingToRename: Pair<String, String>?, onDismissRecordingRename: () -> Unit, attachmentToDelete: String?, onDismissAttachmentDelete: () -> Unit, showNameAttachmentDialog: Pair<Uri, String>?, onDismissNameAttachment: () -> Unit, attachmentToRename: Pair<String, String>?, onDismissAttachmentRename: () -> Unit) {
    val context = LocalContext.current
    if (recordingToDelete != null) {
        AlertDialog(onDismissRequest = onDismissRecordingDelete, title = { Text("Confirmar eliminación") }, text = { Text("¿Estás seguro de que deseas eliminar este audio?") }, confirmButton = { TextButton(onClick = { val updated = uiState.audioRecordings - recordingToDelete; viewModel.updateUiState(uiState.copy(audioRecordings = updated)); onDismissRecordingDelete() }) { Text("Eliminar") } }, dismissButton = { TextButton(onClick = onDismissRecordingDelete) { Text("Cancelar") } })
    }

    if (showNameAudioDialog != null) {
        var audioName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = onDismissNameAudio,
            title = { Text("Nombre del audio") },
            text = { OutlinedTextField(value = audioName, onValueChange = { audioName = it }, label = { Text("Nombre") }) },
            confirmButton = { TextButton(onClick = {
                val finalName = if (audioName.isBlank()) {
                    val nextAudioNum = (uiState.audioRecordings.values
                        .mapNotNull { it.removePrefix("audio ").trim().toIntOrNull() }
                        .maxOrNull() ?: 0) + 1
                    "audio $nextAudioNum"
                } else audioName
                val updated = uiState.audioRecordings + (showNameAudioDialog to finalName)
                viewModel.updateUiState(uiState.copy(audioRecordings = updated))
                onDismissNameAudio()
            }) { Text("Guardar") } })
    }

    if (recordingToRename != null) {
        var audioName by remember { mutableStateOf(recordingToRename.second) }
        AlertDialog(onDismissRequest = onDismissRecordingRename, title = { Text("Renombrar audio") }, text = { OutlinedTextField(value = audioName, onValueChange = { audioName = it }, label = { Text("Nombre") }) }, confirmButton = { TextButton(onClick = {
            val finalName = if (audioName.isBlank()) {
                val nextAudioNum = (uiState.audioRecordings.filterKeys { it != recordingToRename.first }.values
                    .mapNotNull { it.removePrefix("audio ").trim().toIntOrNull() }
                    .maxOrNull() ?: 0) + 1
                "audio $nextAudioNum"
            } else audioName
            val updated = uiState.audioRecordings - recordingToRename.first + (recordingToRename.first to finalName)
            viewModel.updateUiState(uiState.copy(audioRecordings = updated))
            onDismissRecordingRename()
        }) { Text("Guardar") } })
    }

    if (attachmentToDelete != null) {
        AlertDialog(onDismissRequest = onDismissAttachmentDelete, title = { Text("Confirmar eliminación") }, text = { Text("¿Estás seguro de que deseas eliminar este archivo?") }, confirmButton = { TextButton(onClick = { val updated = uiState.attachments - attachmentToDelete; viewModel.updateUiState(uiState.copy(attachments = updated)); onDismissAttachmentDelete() }) { Text("Eliminar") } }, dismissButton = { TextButton(onClick = onDismissAttachmentDelete) { Text("Cancelar") } })
    }

    if (showNameAttachmentDialog != null) {
        var attachmentName by remember(showNameAttachmentDialog) { mutableStateOf(showNameAttachmentDialog.second) }
        AlertDialog(onDismissRequest = onDismissNameAttachment, title = { Text("Nombre del archivo") }, text = { OutlinedTextField(value = attachmentName, onValueChange = { attachmentName = it }, label = { Text("Nombre") }) }, confirmButton = { TextButton(onClick = {
            val (uri, _) = showNameAttachmentDialog
            val finalName = if (attachmentName.isBlank()) {
                val nextAttachNum = (uiState.attachments.values
                    .mapNotNull { it.removePrefix("adjunto ").trim().toIntOrNull() }
                    .maxOrNull() ?: 0) + 1
                "adjunto $nextAttachNum"
            } else attachmentName
            val newFilePath = copyUriToInternalStorage(context, uri, finalName)
            if (newFilePath != null) {
                val updated = uiState.attachments + (newFilePath to finalName)
                viewModel.updateUiState(uiState.copy(attachments = updated))
            }
            onDismissNameAttachment()
        }) { Text("Guardar") } })
    }

    if (attachmentToRename != null) {
        var attachmentName by remember { mutableStateOf(attachmentToRename.second) }
        AlertDialog(onDismissRequest = onDismissAttachmentRename, title = { Text("Renombrar archivo") }, text = { OutlinedTextField(value = attachmentName, onValueChange = { attachmentName = it }, label = { Text("Nombre") }) }, confirmButton = { TextButton(onClick = {
            val finalName = if (attachmentName.isBlank()) {
                val nextAttachNum = (uiState.attachments.filterKeys { it != attachmentToRename.first }.values
                    .mapNotNull { it.removePrefix("adjunto ").trim().toIntOrNull() }
                    .maxOrNull() ?: 0) + 1
                "adjunto $nextAttachNum"
            } else attachmentName
            val updated = uiState.attachments - attachmentToRename.first + (attachmentToRename.first to finalName)
            viewModel.updateUiState(uiState.copy(attachments = updated))
            onDismissAttachmentRename()
        }) { Text("Guardar") } })
    }
}

private fun getFileName(context: Context, uri: Uri): String? {
    return when (uri.scheme) {
        "content" -> {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        cursor.getString(nameIndex)
                    } else null
                } else null
            }
        }
        "file" -> {
            uri.lastPathSegment
        }
        else -> uri.toString().substringAfterLast('/')
    }
}

private fun copyUriToInternalStorage(context: Context, uri: Uri, fileName: String): String? {
    return try {
        val attachmentsDir = File(context.filesDir, "attachments")
        if (!attachmentsDir.exists()) {
            attachmentsDir.mkdirs()
        }
        val file = File(attachmentsDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        file.absolutePath
    } catch (_: Exception) {
        Toast.makeText(context, "Error al guardar el archivo adjunto", Toast.LENGTH_SHORT).show()
        null
    }
}