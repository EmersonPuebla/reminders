package com.example.reminders.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.io.File
import java.util.Locale

data class AttachmentDialogActions(
    val showView: Boolean = true,
    val showEdit: Boolean = false,
    val showDelete: Boolean = false,
    val showShare: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentPreviewDialog(
    path: String,
    name: String,
    onDismiss: () -> Unit,
    actions: AttachmentDialogActions = AttachmentDialogActions(),
    onViewClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    onShareClick: (() -> Unit)? = null
) {
    val file = File(path)
    val fileExtension = file.extension.lowercase(Locale.ROOT)
    val isImage = fileExtension in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp")

    val imageBitmap = remember(path) {
        if (isImage && file.exists()) {
            try {
                BitmapFactory.decodeFile(path)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else null
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (file.exists()) {
                                val sizeInKB = file.length() / 1024
                                val sizeText = if (sizeInKB < 1024) {
                                    "$sizeInKB KB"
                                } else {
                                    String.format("%.1f MB", sizeInKB / 1024f)
                                }
                                Text(
                                    text = sizeText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 400.dp)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = getFileIcon(fileExtension),
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Actions
                if (actions.showView || actions.showEdit || actions.showDelete || actions.showShare) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (actions.showView && onViewClick != null) {
                            OutlinedButton(
                                onClick = onViewClick,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Filled.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ver archivo")
                            }
                        }

                        if (actions.showEdit && onEditClick != null) {
                            OutlinedButton(
                                onClick = onEditClick,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Renombrar")
                            }
                        }

                        if (actions.showShare && onShareClick != null) {
                            OutlinedButton(
                                onClick = onShareClick,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Filled.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Compartir")
                            }
                        }

                        if (actions.showDelete && onDeleteClick != null) {
                            OutlinedButton(
                                onClick = onDeleteClick,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Eliminar")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

private fun getFileIcon(extension: String): ImageVector {
    return when (extension.lowercase()) {
        "jpg", "jpeg", "png", "gif", "bmp", "webp" -> Icons.Filled.Image
        "pdf" -> Icons.Filled.PictureAsPdf
        "doc", "docx" -> Icons.Filled.Description
        "xls", "xlsx" -> Icons.Filled.TableChart
        "txt" -> Icons.AutoMirrored.Filled.TextSnippet
        "zip", "rar", "7z" -> Icons.Filled.FolderZip
        "mp3", "wav", "m4a", "aac" -> Icons.Filled.AudioFile
        "mp4", "avi", "mkv", "mov" -> Icons.Filled.VideoFile
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}
