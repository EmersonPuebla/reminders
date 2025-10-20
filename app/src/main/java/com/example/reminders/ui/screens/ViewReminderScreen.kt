package com.example.reminders.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reminders.ui.AppViewModelProvider
import com.example.reminders.utils.AudioRecorderHelper
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewReminderScreen(
    onBack: () -> Unit,
    onEditClick: (Int) -> Unit,
    viewModel: ViewReminderViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val audioRecorderHelper = remember { AudioRecorderHelper(context) }

    DisposableEffect(Unit) {
        onDispose {
            audioRecorderHelper.stopPlaying()
        }
    }

    val currentOnBack by rememberUpdatedState(onBack)
    LaunchedEffect(viewModel) {
        snapshotFlow { uiState }.first { it != null }

        snapshotFlow { uiState }
            .filter { it == null }
            .collect { currentOnBack() }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Estás seguro de que deseas eliminar este recordatorio?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.deleteReminder()
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (uiState == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        val reminder = uiState!!
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Detalle del Recordatorio") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    actions = {
                        IconButton(onClick = { onEditClick(reminder.id) }) { 
                            Icon(Icons.Filled.Edit, contentDescription = "Editar")
                        }
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
                        }
                    }
                )
            },
            bottomBar = { // Add a bottomBar for the button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                        Text("Volver")
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding) // Apply padding from Scaffold
                    .verticalScroll(rememberScrollState()) // Make the content scrollable
                    .padding(16.dp) // Add padding for the content itself
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = reminder.title,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = reminder.description,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = "Fecha")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(SimpleDateFormat("dd / MM / yyyy", Locale.getDefault()).format(Date(reminder.date)), style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notificación")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (reminder.notify) "Notificación activada" else "Notificación desactivada", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                if (reminder.audioRecordings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Grabaciones de audio", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column {
                        reminder.audioRecordings.forEach { (path, name) ->
                            AudioPlayerItem(
                                audioName = name,
                                onPlayClick = { audioRecorderHelper.playAudio(path) }
                            )
                        }
                    }
                }

                if (reminder.attachments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Archivos adjuntos", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column {
                        reminder.attachments.forEach { (uri, name) ->
                            AttachmentItem(
                                attachmentName = name,
                                onViewClick = {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse(uri)
                                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    }
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioPlayerItem(audioName: String, onPlayClick: () -> Unit) {
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
        }
    }
}

@Composable
private fun AttachmentItem(attachmentName: String, onViewClick: () -> Unit) {
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
            IconButton(onClick = onViewClick) {
                Icon(Icons.Filled.Visibility, contentDescription = "Ver archivo")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = attachmentName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
