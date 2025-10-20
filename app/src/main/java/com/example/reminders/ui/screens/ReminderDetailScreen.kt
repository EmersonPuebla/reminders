package com.example.reminders.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reminders.ui.AppViewModelProvider
import com.example.reminders.utils.AudioRecorderHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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
    var recordingTimeSeconds by remember { mutableStateOf(0) }
    var showExitConfirmationDialog by remember { mutableStateOf(false) }
    var showNameAudioDialog by remember { mutableStateOf<String?>(null) }

    val audioRecorderHelper = remember { AudioRecorderHelper(context) }

    val hasUnsavedChanges = uiState.title.isNotBlank() || uiState.description.isNotBlank() || uiState.date != 0L || uiState.audioRecordings.isNotEmpty()

    fun handleBackNavigation() {
        if (hasUnsavedChanges) {
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
            confirmButton = {
                TextButton(onClick = onBack) {
                    Text("Descartar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmationDialog = false }) {
                    Text("Seguir editando")
                }
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            isRecording = true
            audioRecorderHelper.startRecording()
        }
    }

    fun Int.toMMSS(): String {
        val minutes = this / 60
        val seconds = this % 60
        return String.format("%02d\n%02d", minutes, seconds)
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

    RecordingTimer(isRecording = isRecording) { newTime ->
        recordingTimeSeconds = newTime
    }

    if (recordingToDelete != null) {
        AlertDialog(
            onDismissRequest = { recordingToDelete = null },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Estás seguro de que deseas eliminar este audio?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        recordingToDelete?.let { recording ->
                            val updatedAudioRecordings = uiState.audioRecordings - recording
                            viewModel.updateUiState(uiState.copy(audioRecordings = updatedAudioRecordings))
                        }
                        recordingToDelete = null
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordingToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showNameAudioDialog != null) {
        var audioName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNameAudioDialog = null },
            title = { Text("Nombre del audio") },
            text = {
                OutlinedTextField(
                    value = audioName,
                    onValueChange = { audioName = it },
                    label = { Text("Nombre") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNameAudioDialog?.let {
                            val updatedAudioRecordings = uiState.audioRecordings + (it to audioName)
                            viewModel.updateUiState(uiState.copy(audioRecordings = updatedAudioRecordings))
                        }
                        showNameAudioDialog = null
                    }
                ) {
                    Text("Guardar")
                }
            }
        )
    }

    if (recordingToRename != null) {
        var audioName by remember { mutableStateOf(recordingToRename!!.second) }
        AlertDialog(
            onDismissRequest = { recordingToRename = null },
            title = { Text("Renombrar audio") },
            text = {
                OutlinedTextField(
                    value = audioName,
                    onValueChange = { audioName = it },
                    label = { Text("Nombre") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        recordingToRename?.let {
                            val updatedAudioRecordings = uiState.audioRecordings - it.first + (it.first to audioName)
                            viewModel.updateUiState(uiState.copy(audioRecordings = updatedAudioRecordings))
                        }
                        recordingToRename = null
                    }
                ) {
                    Text("Guardar")
                }
            }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Crear Recordatorio") }) },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = ::handleBackNavigation) {
                    Text("Cancelar")
                }

                Button(
                    onClick = {
                        if (isRecording) {
                            isRecording = false
                            audioRecorderHelper.stopRecording()?.let { filePath ->
                                showNameAudioDialog = filePath
                            }
                            recordingTimeSeconds = 0
                        } else {
                            recordingTimeSeconds = 0
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier.size(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isRecording) {
                        Text(
                            text = recordingTimeSeconds.toMMSS(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.onError
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Grabar audio"
                        )
                    }
                }

                Button(onClick = {
                    if (uiState.title.isBlank() || uiState.date == 0L) {
                        Toast.makeText(context, "El título y la fecha son obligatorios.", Toast.LENGTH_LONG).show()
                    } else {
                        coroutineScope.launch {
                            viewModel.saveReminder()
                            onBack()
                        }
                    }
                }) {
                    Text("Guardar")
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
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.description,
                onValueChange = { viewModel.updateUiState(uiState.copy(description = it)) },
                label = { Text("Detalles") },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 120.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            val showDatePicker = remember { mutableStateOf(false) }
            Button(onClick = { showDatePicker.value = true }) {
                Text(text = if (uiState.date != 0L) SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(uiState.date)) else "Seleccionar fecha")
            }

            if (showDatePicker.value) {
                val datePickerState = rememberDatePickerState()
                DatePickerDialog(
                    onDismissRequest = { showDatePicker.value = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let {
                                viewModel.updateUiState(uiState.copy(date = it))
                            }
                            showDatePicker.value = false
                        }) {
                            Text("Aceptar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker.value = false }) {
                            Text("Cancelar")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = uiState.notify, onCheckedChange = { viewModel.updateUiState(uiState.copy(notify = it)) })
                Spacer(modifier = Modifier.width(8.dp))
                Text("Notificarme")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Grabaciones de audio", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Column {
                uiState.audioRecordings.forEach { (path, name) ->
                    AudioPlayer(
                        audioName = name,
                        onPlayClick = { audioRecorderHelper.playAudio(path) },
                        onDeleteClick = { recordingToDelete = path },
                        onEditClick = { recordingToRename = path to name }
                    )
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            audioRecorderHelper.stopPlaying()
        }
    }
}

@Composable
fun AudioPlayer(
    audioName: String,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPlayClick) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Reproducir audio")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = audioName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            IconButton(onClick = onEditClick) {
                Icon(Icons.Filled.Edit, contentDescription = "Renombrar audio")
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar audio")
            }
        }
    }
}
