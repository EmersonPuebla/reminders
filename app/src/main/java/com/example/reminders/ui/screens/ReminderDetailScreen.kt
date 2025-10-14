package com.example.reminders.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reminders.utils.AudioRecorderHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.*

// Data class to hold audio recording information
data class AudioRecording(val name: String, val filePath: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDetailScreen(onBack: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var isNotifying by remember { mutableStateOf(false) }
    val selectedDate = remember { mutableStateOf<Long?>(null) }
    val showDatePicker = remember { mutableStateOf(false) }
    var audioRecordings by remember { mutableStateOf<List<AudioRecording>>(emptyList()) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingToDelete by remember { mutableStateOf<AudioRecording?>(null) }
    var recordingTimeSeconds by remember { mutableStateOf(0) } // 🕑 Estado para el contador

    val context = LocalContext.current
    val audioRecorderHelper = remember { AudioRecorderHelper(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            isRecording = true
            audioRecorderHelper.startRecording()
        }
    }

    /**
     * Función de extensión para formatear segundos a MM:SS
     */
    fun Int.toMMSS(): String {
        val minutes = this / 60
        val seconds = this % 60
        return String.format("%02d\n%02d", minutes, seconds)
    }

    /**
     * Composable que maneja el contador de tiempo
     */
    @Composable
    fun RecordingTimer(isRecording: Boolean, onTimeUpdate: (Int) -> Unit) {
        // Ejecuta el bloque solo cuando isRecording cambia
        LaunchedEffect(isRecording) {
            if (isRecording) {
                var time = 0
                // Continuar contando hasta que el LaunchedEffect sea cancelado (isRecording = false)
                while (isActive) {
                    delay(1000) // Espera 1 segundo
                    time++
                    onTimeUpdate(time)
                }
            }
        }
    }

    // 🔥 IMPORTANTE: Llamada al Composable RecordingTimer para que el tiempo se actualice
    RecordingTimer(isRecording = isRecording) { newTime ->
        recordingTimeSeconds = newTime
    }

    if (recordingToDelete != null) {
        AlertDialog(
            onDismissRequest = { recordingToDelete = null },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Estás seguro de que deseas eliminar '${recordingToDelete?.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        recordingToDelete?.let { recording ->
                            audioRecordings = audioRecordings - recording
                            // Aquí podrías añadir lógica para eliminar el archivo físico
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
                Button(onClick = onBack) {
                    Text("Cancelar")
                }

                // Botón de grabación/detención (CORREGIDO)
                Button(
                    onClick = {
                        if (isRecording) {
                            // Lógica para detener la grabación
                            isRecording = false
                            audioRecorderHelper.stopRecording()?.let { filePath ->
                                val newRecording = AudioRecording(
                                    name = "Audio ${audioRecordings.size + 1}",
                                    filePath = filePath
                                )
                                audioRecordings = audioRecordings + newRecording
                            }
                            // Resetear el tiempo al detener
                            recordingTimeSeconds = 0
                        } else {
                            // Lógica para solicitar permiso e iniciar
                            recordingTimeSeconds = 0 // Asegurar que el tiempo esté en 0 antes de iniciar
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
                        // Muestra el tiempo transcurrido en MM:SS
                        Text(
                            text = recordingTimeSeconds.toMMSS(),
                            // CORRECCIÓN: Usar un estilo pequeño para que quepa en el botón (Propuesta 1)
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.onError
                        )
                    } else {
                        // Muestra el icono de Play/Grabar
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Grabar audio"
                        )
                    }
                }

                Button(onClick = onBack) {
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
                value = title,
                onValueChange = { title = it },
                label = { Text("Título") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = details,
                onValueChange = { details = it },
                label = { Text("Detalles") },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 120.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Selector de fecha
            Button(onClick = { showDatePicker.value = true }) {
                Text(text = selectedDate.value?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it)) } ?: "Seleccionar fecha")
            }

            if (showDatePicker.value) {
                val datePickerState = rememberDatePickerState()
                DatePickerDialog(
                    onDismissRequest = { showDatePicker.value = false },
                    confirmButton = {
                        TextButton(onClick = {
                            selectedDate.value = datePickerState.selectedDateMillis
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
                Checkbox(checked = isNotifying, onCheckedChange = { isNotifying = it })
                Spacer(modifier = Modifier.width(8.dp))
                Text("Notificarme")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Grabaciones de audio", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Column {
                audioRecordings.forEach { recording ->
                    AudioPlayer(
                        audioRecording = recording,
                        onPlayClick = { audioRecorderHelper.playAudio(recording.filePath) },
                        onDeleteClick = { recordingToDelete = recording }
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
    audioRecording: AudioRecording,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit
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
                text = audioRecording.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar audio")
            }
        }
    }
}