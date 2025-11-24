package com.example.reminders.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reminders.ui.AppViewModelProvider
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val selectedTheme by viewModel.theme.collectAsState()
    val serverAddress by viewModel.serverAddress.collectAsState()
    val serverPort by viewModel.serverPort.collectAsState()
    val useHttps by viewModel.useHttps.collectAsState()
    val syncEnabled by viewModel.syncEnabled.collectAsState()
    val syncInterval by viewModel.syncInterval.collectAsState()

    var address by remember(serverAddress) { mutableStateOf(serverAddress) }
    var port by remember(serverPort) { mutableStateOf(serverPort) }
    var isHttps by remember(useHttps) { mutableStateOf(useHttps) }
    var isSyncEnabled by remember(syncEnabled) { mutableStateOf(syncEnabled) }
    var interval by remember(syncInterval) { mutableStateOf(syncInterval.toString()) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Tema", style = MaterialTheme.typography.titleLarge)
            Theme.values().forEach { theme ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedTheme == theme,
                        onClick = { viewModel.updateTheme(theme) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(theme.name)
                }
            }

            HorizontalDivider()

            Text(text = "Servidor", style = MaterialTheme.typography.titleLarge)

            Text("Dirección")
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                placeholder = { Text("10.10.10.25") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("Puerto")
            OutlinedTextField(
                value = port,
                onValueChange = { port = it },
                placeholder = { Text("8040") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Usar HTTPS")
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = isHttps,
                    onCheckedChange = { isHttps = it }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    scope.launch {
                        val success = viewModel.testConnection()
                        val message = if (success) "Conexión exitosa" else "Error en la conexión"
                        snackbarHostState.showSnackbar(message)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Probar conexión")
            }

            HorizontalDivider()

            Text(text = "Sincronización", style = MaterialTheme.typography.titleLarge)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Activar sincronización")
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = isSyncEnabled,
                    onCheckedChange = { isSyncEnabled = it }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = interval,
                onValueChange = { interval = it },
                label = { Text("Intervalo de sincronización (minutos)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                enabled = isSyncEnabled
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.saveConnectionDetails(address, port, isHttps)
                    viewModel.saveSyncSettings(isSyncEnabled, interval.toIntOrNull() ?: 15)
                    scope.launch {
                        snackbarHostState.showSnackbar("Ajustes guardados")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar")
            }
        }
    }
}

enum class Theme {
    LIGHT,
    DARK,
    SYSTEM
}
