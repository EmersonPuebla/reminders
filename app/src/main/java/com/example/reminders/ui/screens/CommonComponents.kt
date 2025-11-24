package com.example.reminders.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.reminders.utils.AudioRecorderHelper

@Composable
fun AudioPlayer(
    audioPath: String,
    audioName: String,
    audioRecorderHelper: AudioRecorderHelper,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val isPlaying by audioRecorderHelper.isPlaying.collectAsState()
    val progress by audioRecorderHelper.progress.collectAsState()
    val duration by audioRecorderHelper.duration.collectAsState()
    val currentPlayingPath by audioRecorderHelper.currentPlayingPath.collectAsState()

    val isCurrentAudio = currentPlayingPath == audioPath

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { audioRecorderHelper.playAudio(audioPath) }) {
                    Icon(
                        imageVector = if (isPlaying && isCurrentAudio) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying && isCurrentAudio) "Pausar audio" else "Reproducir audio"
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = audioName, modifier = Modifier.weight(1f), style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
                IconButton(onClick = onEditClick) { Icon(Icons.Filled.Edit, "Renombrar audio") }
                IconButton(onClick = onDeleteClick) { Icon(Icons.Filled.Delete, "Eliminar audio") }
            }
            if (isCurrentAudio) {
                Slider(
                    value = progress.toFloat(),
                    onValueChange = { audioRecorderHelper.seekTo(it.toInt()) },
                    valueRange = 0f..duration.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun AttachmentItem(
    attachmentName: String,
    onViewClick: () -> Unit,
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
            IconButton(onClick = onViewClick) { Icon(Icons.Filled.Visibility, "Ver archivo") }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = attachmentName,
                modifier = Modifier.weight(1f),
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
            )
            IconButton(onClick = onEditClick) { Icon(Icons.Filled.Edit, "Renombrar archivo") }
            IconButton(onClick = onDeleteClick) { Icon(Icons.Filled.Delete, "Eliminar archivo") }
        }
    }
}
