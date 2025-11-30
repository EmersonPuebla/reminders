package com.example.reminders.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.reminders.utils.AudioRecorderHelper
import java.util.Locale

@Composable
fun AudioPlayer(
    audioPath: String,
    audioName: String,
    audioRecorderHelper: AudioRecorderHelper,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    showButtons: Boolean = true
) {
    val isPlaying by audioRecorderHelper.isPlaying.collectAsState()
    val progress by audioRecorderHelper.progress.collectAsState()
    val duration by audioRecorderHelper.duration.collectAsState()
    val currentPlayingPath by audioRecorderHelper.currentPlayingPath.collectAsState()
    val playbackSpeed by audioRecorderHelper.playbackSpeed.collectAsState()

    val isCurrentAudio = currentPlayingPath == audioPath

    var isDragging by remember { mutableStateOf(false) }
    var tempProgress by remember { mutableFloatStateOf(0f) }

    // Formatear tiempo en mm:ss
    fun formatTime(milliseconds: Int): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
    }

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
                Text(
                    text = audioName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )
                if (showButtons) {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Filled.Edit, "Renombrar audio")
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Filled.Delete, "Eliminar audio")
                    }
                }
            }

            if (isCurrentAudio) {
                Spacer(modifier = Modifier.height(8.dp))

                // Slider con control de pausa temporal
                Slider(
                    value = if (isDragging) tempProgress else progress.toFloat(),
                    onValueChange = { newValue ->
                        if (!isDragging) {
                            audioRecorderHelper.wasPlayingBeforeDrag = isPlaying
                            isDragging = true
                        }
                        tempProgress = newValue
                        audioRecorderHelper.pauseAudio()
                    },
                    onValueChangeFinished = {
                        audioRecorderHelper.seekTo(tempProgress.toInt(), audioRecorderHelper.wasPlayingBeforeDrag)
                        isDragging = false
                    },
                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                    modifier = Modifier.fillMaxWidth()
                )

                // Fila con tiempos y velocidad
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${formatTime(if (isDragging) tempProgress.toInt() else progress)} / ${formatTime(duration)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Chip de velocidad
                    SuggestionChip(
                        onClick = { audioRecorderHelper.cyclePlaybackSpeed() },
                        label = {
                            Text(
                                text = "${playbackSpeed}x",
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Speed,
                                contentDescription = "Velocidad de reproducción",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}