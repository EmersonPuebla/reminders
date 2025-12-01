package com.example.reminders.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import com.example.reminders.ui.AppViewModelProvider
import com.example.reminders.ui.components.AttachmentDialogActions
import com.example.reminders.ui.components.AttachmentPreviewDialog
import com.example.reminders.ui.components.AudioPlayer
import com.example.reminders.utils.AudioRecorderHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReminderView(
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
    var showNameAudioDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showNameAttachmentDialog by remember { mutableStateOf<Pair<Uri, String>?>(null) }
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedAttachment by remember { mutableStateOf<Pair<String, String>?>(null) }


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
        uri?.let { it ->
            val fileName = getFileName(context, it) ?: "Archivo"
            val mimeType = context.contentResolver.getType(it)
            if (mimeType?.startsWith("audio/") == true) {
                val newPath = viewModel.copyFileToInternalStorage(it, "recordings", fileName)
                newPath?.let {
                    showNameAudioDialog = it to fileName
                }
            } else {
                showNameAttachmentDialog = it to fileName
            }
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempImageUri?.let {
                val fileName = getFileName(context, it) ?: "Imagen"
                showNameAttachmentDialog = it to fileName
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val imageFile = File(context.cacheDir, "temp_image.jpg")
            tempImageUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
            takePictureLauncher.launch(tempImageUri)
        } else {
            Toast.makeText(context, "El permiso de la cámara es necesario", Toast.LENGTH_SHORT).show()
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

    selectedAttachment?.let { (path, name) ->
        // NEW
            AttachmentPreviewDialog(
                path = path,
                name = name,
                onDismiss = { selectedAttachment = null },
                actions = AttachmentDialogActions(
                    showView = true,
                    showEdit = true,
                    showDelete = true,
                    showShare = false
                ),
                onViewClick = {
                    selectedAttachment = null // Cerrar primero
                    try {
                        val file = File(path)
                        if (!file.exists()) {
                            Toast.makeText(context, "El archivo no existe", Toast.LENGTH_SHORT).show()
                            return@AttachmentPreviewDialog
                        }

                        val authority = "${context.packageName}.fileprovider"
                        val uri = FileProvider.getUriForFile(context, authority, file)
                        val mimeType = context.contentResolver.getType(uri) ?: "*/*"

                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, mimeType)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }

                        // Verificar que hay una app que pueda manejar el intent
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        } else {
                            Toast.makeText(
                                context,
                                "No se encontró una aplicación para abrir este archivo.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Error al abrir el archivo: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onEditClick = {
                    attachmentToRename = selectedAttachment
                    selectedAttachment = null
                },
                onDeleteClick = {
                    attachmentToDelete = selectedAttachment?.first
                    selectedAttachment = null
                },
                onShareClick = { /* compartir */ }
            )
    }

    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
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
                            onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                            modifier = Modifier.size(64.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Tomar foto")
                        }

                        Button(
                            onClick = {
                                if (isRecording) {
                                    isRecording = false
                                    audioRecorderHelper.stopRecording()?.let { showNameAudioDialog = it to "Grabación" }
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
                                Icon(Icons.Filled.Mic, contentDescription = "Grabar audio")
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = { viewModel.updateUiState(uiState.copy(title = it)) },
                    label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = { viewModel.updateUiState(uiState.copy(description = it)) },
                    label = { Text("Detalles") },
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 120.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Fecha del recordatorio",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        var showDatePicker by remember { mutableStateOf(false) }
                        var showTimePicker by remember { mutableStateOf(false) }
                        var tempSelectedDate by remember { mutableStateOf<Long?>(null) }

                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.date != 0L)
                                    SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy 'a las' HH:mm", Locale("es", "ES")).format(Date(uiState.date))
                                else "Seleccionar fecha y hora"
                            )
                        }

                        if (showDatePicker) {
                            val datePickerState = rememberDatePickerState(
                                initialSelectedDateMillis = if (uiState.date != 0L) {
                                    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                                    cal.timeInMillis = uiState.date
                                    cal.timeInMillis
                                } else {
                                    System.currentTimeMillis()
                                }
                            )

                            DatePickerDialog(
                                onDismissRequest = { showDatePicker = false },
                                confirmButton = {
                                    TextButton(onClick = {
                                        datePickerState.selectedDateMillis?.let { selectedMillis ->
                                            val localCal = Calendar.getInstance()
                                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                                            utcCal.timeInMillis = selectedMillis

                                            localCal.set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                            localCal.set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                            localCal.set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                                            localCal.set(Calendar.HOUR_OF_DAY, 0)
                                            localCal.set(Calendar.MINUTE, 0)
                                            localCal.set(Calendar.SECOND, 0)
                                            localCal.set(Calendar.MILLISECOND, 0)

                                            tempSelectedDate = localCal.timeInMillis
                                        }
                                        showDatePicker = false
                                        if (tempSelectedDate != null) {
                                            showTimePicker = true
                                        }
                                    }) { Text("Aceptar") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
                                }
                            ) {
                                DatePicker(state = datePickerState)
                            }
                        }

                        if (showTimePicker) {
                            val calendar = Calendar.getInstance()
                            if (uiState.date != 0L) {
                                calendar.timeInMillis = uiState.date
                            }
                            val timePickerState = rememberTimePickerState(
                                initialHour = calendar.get(Calendar.HOUR_OF_DAY),
                                initialMinute = calendar.get(Calendar.MINUTE),
                                is24Hour = true
                            )
                            AlertDialog(
                                onDismissRequest = { showTimePicker = false },
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
                                                timeInMillis = tempSelectedDate!!
                                                set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                                set(Calendar.MINUTE, timePickerState.minute)
                                                set(Calendar.SECOND, 0)
                                                set(Calendar.MILLISECOND, 0)
                                            }
                                            viewModel.updateUiState(uiState.copy(date = newCal.timeInMillis))
                                            showTimePicker = false
                                        }
                                    ) { Text("Aceptar") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Notificación",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Switch(
                                checked = uiState.notify,
                                onCheckedChange = { viewModel.updateUiState(uiState.copy(notify = it)) }
                            )
                        }

                        if (uiState.notify) {
                            Spacer(modifier = Modifier.height(12.dp))

                            var showNotifyDatePicker by remember { mutableStateOf(false) }
                            var showNotifyTimePicker by remember { mutableStateOf(false) }
                            var selectedDate by remember { mutableStateOf<Long?>(null) }

                            OutlinedButton(
                                onClick = { showNotifyDatePicker = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (uiState.notifyDate != 0L)
                                        SimpleDateFormat("EEEE, dd/MM/yyyy 'a las' HH:mm", Locale("es", "ES")).format(Date(uiState.notifyDate))
                                    else "Seleccionar fecha y hora"
                                )
                            }

                            if (showNotifyDatePicker) {
                                val datePickerState = rememberDatePickerState(
                                    initialSelectedDateMillis = if (uiState.notifyDate != 0L) {
                                        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                                        cal.timeInMillis = uiState.notifyDate
                                        cal.timeInMillis
                                    } else {
                                        System.currentTimeMillis()
                                    }
                                )

                                DatePickerDialog(
                                    onDismissRequest = { showNotifyDatePicker = false },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            datePickerState.selectedDateMillis?.let { selectedMillis ->
                                                val localCal = Calendar.getInstance()
                                                val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                                                utcCal.timeInMillis = selectedMillis

                                                localCal.set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                                localCal.set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                                localCal.set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                                                localCal.set(Calendar.HOUR_OF_DAY, 0)
                                                localCal.set(Calendar.MINUTE, 0)
                                                localCal.set(Calendar.SECOND, 0)
                                                localCal.set(Calendar.MILLISECOND, 0)

                                                selectedDate = localCal.timeInMillis
                                            }
                                            showNotifyDatePicker = false
                                            if (selectedDate != null) {
                                                showNotifyTimePicker = true
                                            }
                                        }) { Text("Aceptar") }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showNotifyDatePicker = false }) { Text("Cancelar") }
                                    }
                                ) {
                                    DatePicker(state = datePickerState)
                                }
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
                    }
                }

                if (uiState.audioRecordings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Grabaciones de audio", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column {
                        uiState.audioRecordings.forEach { (path, name) ->
                            AudioPlayer(
                                audioPath = path,
                                audioName = name,
                                audioRecorderHelper = audioRecorderHelper,
                                onDeleteClick = { recordingToDelete = path },
                                onEditClick = { recordingToRename = path to name }
                            )
                        }
                    }
                }

                if (uiState.attachments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Archivos adjuntos", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 100.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 500.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.attachments.toList()) { (path, name) ->
                            EditableAttachmentGridItem(
                                path = path,
                                name = name,
                                onClick = { selectedAttachment = path to name }
                            )

                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) { onDispose { audioRecorderHelper.stopPlaying() } }
}

@Composable
private fun EditableAttachmentGridItem(path: String, name: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                SubcomposeAsyncImage(
                    model = path,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    error = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                            contentDescription = "File type icon",
                            modifier = Modifier.size(48.dp)
                        )
                    }
                )
            }
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

