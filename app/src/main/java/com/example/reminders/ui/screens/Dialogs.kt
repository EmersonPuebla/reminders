package com.example.reminders.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import java.io.FileOutputStream

@Composable
fun HandleDialogs(
    uiState: ReminderUiState, viewModel: ReminderDetailViewModel, recordingToDelete: String?, onDismissRecordingDelete: () -> Unit, showNameAudioDialog: Pair<String, String>?, onDismissNameAudio: () -> Unit, recordingToRename: Pair<String, String>?, onDismissRecordingRename: () -> Unit, attachmentToDelete: String?, onDismissAttachmentDelete: () -> Unit, showNameAttachmentDialog: Pair<Uri, String>?, onDismissNameAttachment: () -> Unit, attachmentToRename: Pair<String, String>?, onDismissAttachmentRename: () -> Unit) {
    val context = LocalContext.current
    if (recordingToDelete != null) {
        AlertDialog(onDismissRequest = onDismissRecordingDelete, title = { Text("Confirmar eliminación") }, text = { Text("¿Estás seguro de que deseas eliminar este audio?") }, confirmButton = { TextButton(onClick = { val updated = uiState.audioRecordings - recordingToDelete; viewModel.updateUiState(uiState.copy(audioRecordings = updated)); onDismissRecordingDelete() }) { Text("Eliminar") } }, dismissButton = { TextButton(onClick = onDismissRecordingDelete) { Text("Cancelar") } })
    }

    if (showNameAudioDialog != null) {
        var audioName by remember(showNameAudioDialog) { mutableStateOf(showNameAudioDialog.second) }
        AlertDialog(
            onDismissRequest = onDismissNameAudio,
            title = { Text("Nombre del audio") },
            text = { OutlinedTextField(value = audioName, onValueChange = { audioName = it }, label = { Text("Nombre") }) },
            confirmButton = { TextButton(onClick = {
                val (path, _) = showNameAudioDialog
                val finalName = audioName.ifBlank {
                    val nextAudioNum = (uiState.audioRecordings.values
                        .mapNotNull { it.removePrefix("audio ").trim().toIntOrNull() }
                        .maxOrNull() ?: 0) + 1
                    "audio $nextAudioNum"
                }
                val updated = uiState.audioRecordings + (path to finalName)
                viewModel.updateUiState(uiState.copy(audioRecordings = updated))
                onDismissNameAudio()
            }) { Text("Guardar") } })
    }

    if (recordingToRename != null) {
        var audioName by remember { mutableStateOf(recordingToRename.second) }
        AlertDialog(onDismissRequest = onDismissRecordingRename, title = { Text("Renombrar audio") }, text = { OutlinedTextField(value = audioName, onValueChange = { audioName = it }, label = { Text("Nombre") }) }, confirmButton = { TextButton(onClick = {
            val finalName = audioName.ifBlank {
                val nextAudioNum = (uiState.audioRecordings.filterKeys { it != recordingToRename.first }.values
                    .mapNotNull { it.removePrefix("audio ").trim().toIntOrNull() }
                    .maxOrNull() ?: 0) + 1
                "audio $nextAudioNum"
            }
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
            val finalName = attachmentName.ifBlank {
                val nextAttachNum = (uiState.attachments.values
                    .mapNotNull { it.removePrefix("adjunto ").trim().toIntOrNull() }
                    .maxOrNull() ?: 0) + 1
                "adjunto $nextAttachNum"
            }
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
            val finalName = attachmentName.ifBlank {
                val nextAttachNum = (uiState.attachments.filterKeys { it != attachmentToRename.first }.values
                    .mapNotNull { it.removePrefix("adjunto ").trim().toIntOrNull() }
                    .maxOrNull() ?: 0) + 1
                "adjunto $nextAttachNum"
            }
            val updated = uiState.attachments - attachmentToRename.first + (attachmentToRename.first to finalName)
            viewModel.updateUiState(uiState.copy(attachments = updated))
            onDismissAttachmentRename()
        }) { Text("Guardar") } })
    }
}

fun getFileName(context: Context, uri: Uri): String? {
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

fun copyUriToInternalStorage(context: Context, uri: Uri, fileName: String): String? {
    return try {
        val attachmentsDir = java.io.File(context.filesDir, "attachments")
        if (!attachmentsDir.exists()) {
            attachmentsDir.mkdirs()
        }
        val file = java.io.File(attachmentsDir, fileName)
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
