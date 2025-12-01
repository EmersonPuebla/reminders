package com.example.reminders.ui.screens

import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.reminders.data.Reminder
import com.example.reminders.ui.components.AttachmentDialogActions
import com.example.reminders.ui.components.AttachmentPreviewDialog
import com.example.reminders.ui.components.AudioPlayer
import com.example.reminders.utils.AudioRecorderHelper
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReadReminderView(
    modifier: Modifier = Modifier,
    reminder: Reminder,
    onEditClick: (Int) -> Unit
) {
    val context = LocalContext.current
    val audioRecorderHelper = remember { AudioRecorderHelper(context) }

    DisposableEffect(Unit) {
        onDispose {
            audioRecorderHelper.stopPlaying()
        }
    }

    val tabs = listOf("Detalles", "Adjuntos", "Audios")
    val tabIcons = listOf(Icons.AutoMirrored.Filled.List, Icons.Filled.AttachFile, Icons.Filled.GraphicEq)
    val pagerState = rememberPagerState { tabs.size }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = { onEditClick(reminder.id) }) {
                Icon(Icons.Filled.Edit, contentDescription = "Editar")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                        icon = { Icon(tabIcons[index], contentDescription = title) },
                        text = { Text(title) }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> DetailsTab(reminder = reminder)
                    1 -> AttachmentsTab(reminder = reminder)
                    2 -> AudiosTab(reminder = reminder, audioRecorderHelper = audioRecorderHelper)
                }
            }
        }
    }
}

@Composable
fun DetailsTab(reminder: Reminder) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = reminder.title,
            style = MaterialTheme.typography.headlineLarge,
        )
        if (reminder.description.isNotBlank()) {
            Text(
                text = reminder.description,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        HorizontalDivider()

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DateRange, contentDescription = "Fecha del recordatorio")
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Recordatorio para el ${
                    SimpleDateFormat(
                        "EEEE, dd 'de' MMMM 'de' yyyy 'a las' HH:mm",
                        Locale("es", "ES")
                    ).format(Date(reminder.date))
                }",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Notifications, contentDescription = "Notificación")
            Spacer(modifier = Modifier.width(8.dp))
            if (reminder.notify && reminder.notifyDate != 0L) {
                Text(
                    "Notificación activada para el ${
                        SimpleDateFormat(
                            "EEEE, dd 'de' MMMM 'de' yyyy 'a las' HH:mm",
                            Locale("es", "ES")
                        ).format(
                            Date(
                                reminder.notifyDate ?: 0L
                            )
                        )
                    }",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text("Notificación desactivada", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun AttachmentsTab(reminder: Reminder) {
    val context = LocalContext.current
    var selectedAttachment by remember { mutableStateOf<Pair<String, String>?>(null) }

    selectedAttachment?.let { (path, name) ->
        AttachmentPreviewDialog(
            path = path,
            name = name,
            onDismiss = { selectedAttachment = null },
            actions = AttachmentDialogActions(
                showView = true,
                showEdit = false,
                showDelete = false,
                showShare = false
            ),
            onViewClick = {
                selectedAttachment = null
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
            }
        )
    }

    if (reminder.attachments.isEmpty()) {
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp), contentAlignment = Alignment.Center) {
            Text("No hay archivos adjuntos")
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(reminder.attachments.toList()) { (path, name) ->
                AttachmentGridItem(
                    path = path,
                    name = name,
                    onClick = {
                        selectedAttachment = path to name
                    }
                )
            }
        }
    }
}

@Composable
fun AttachmentGridItem(path: String, name: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val imageBitmap = remember(path) {
                try {
                    BitmapFactory.decodeFile(path)?.asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            }
            val fileExtension = File(path).extension.lowercase(Locale.ROOT)

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (imageBitmap != null && (fileExtension == "jpg" || fileExtension == "jpeg" || fileExtension == "png")) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val fallbackIcon = when (fileExtension) {
                        "pdf" -> Icons.Filled.PictureAsPdf
                        else -> Icons.AutoMirrored.Filled.InsertDriveFile
                    }
                    Icon(
                        imageVector = fallbackIcon,
                        contentDescription = "File type icon",
                        modifier = Modifier.size(48.dp)
                    )
                }
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

@Composable
fun AudiosTab(reminder: Reminder, audioRecorderHelper: AudioRecorderHelper) {
    if (reminder.audioRecordings.isEmpty()) {
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp), contentAlignment = Alignment.Center) {
            Text("No hay grabaciones de audio")
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            reminder.audioRecordings.forEach { (path, name) ->
                AudioPlayer(
                    audioPath = path,
                    audioName = name,
                    audioRecorderHelper = audioRecorderHelper,
                    onDeleteClick = {},
                    onEditClick = {},
                    showButtons = false
                )
            }
        }
    }
}

