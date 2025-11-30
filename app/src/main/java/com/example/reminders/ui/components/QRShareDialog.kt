package com.example.reminders.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONObject
import androidx.core.graphics.set
import androidx.core.graphics.createBitmap

@Composable
fun QRShareDialog(
    serverConfig: ServerConfig,
    onDismiss: () -> Unit
) {
    val qrBitmap = remember(serverConfig) {
        generateQRCode(serverConfig)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Column {
                // Barra superior (sin cambios)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Compartir configuración",
                            style = MaterialTheme.typography.titleLarge
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                        }
                    }
                }

                // Área del código QR (CORRECCIÓN AQUÍ)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // 🚨 CAMBIO CLAVE: Eliminar .height(450.dp)
                        // Dejamos que el contenido del Column interno dicte la altura.
                        .wrapContentHeight(), // Asegura que la altura se ajuste al contenido.
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        // QR Code (tamaño fijo de 280.dp está bien)
                        qrBitmap?.let { bitmap ->
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 4.dp
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Código QR de configuración",
                                    modifier = Modifier
                                        .size(280.dp)
                                        .padding(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Información de la configuración (tamaño flexible)
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                ConfigInfoRow("Dirección:", serverConfig.address)

                                Spacer(modifier = Modifier.height(8.dp))

                                ConfigInfoRow("Puerto:", serverConfig.port)

                                Spacer(modifier = Modifier.height(8.dp))

                                ConfigInfoRow("HTTPS:", if (serverConfig.useHttps) "Sí" else "No")
                            }
                        }
                    }
                }

                // Instrucciones (sin cambios)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "Escanea este código desde otro dispositivo para importar la configuración",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfigInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

private fun generateQRCode(config: ServerConfig): Bitmap? {
    return try {
        val json = JSONObject().apply {
            put("address", config.address)
            put("port", config.port)
            put("https", config.useHttps)
        }.toString()

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(
            json,
            BarcodeFormat.QR_CODE,
            512,
            512
        )

        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = createBitmap(width, height, Bitmap.Config.RGB_565)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap[x, y] =
                    if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
        }

        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}