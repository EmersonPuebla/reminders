package com.example.reminders.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reminders.ui.AppViewModelProvider
import com.example.reminders.ui.components.QRScannerDialog
import com.example.reminders.ui.components.QRShareDialog
import com.example.reminders.ui.components.ServerConfig
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
    val showSyncButton by viewModel.showSyncButton.collectAsState()

    var showQRScanner by remember { mutableStateOf(false) }
    var showQRShare by remember { mutableStateOf(false) }

    var address by remember(serverAddress) { mutableStateOf(serverAddress) }
    var port by remember(serverPort) { mutableStateOf(serverPort) }
    var isHttps by remember(useHttps) { mutableStateOf(useHttps) }
    var isSyncEnabled by remember(syncEnabled) { mutableStateOf(syncEnabled) }
    var interval by remember(syncInterval) { mutableStateOf(syncInterval.toString()) }
    var isShowSyncButton by remember(showSyncButton) { mutableStateOf(showSyncButton) }
    var themeExpanded by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Sección: Apariencia
            SettingsSection(
                title = "Apariencia",
                icon = Icons.Filled.Palette
            ) {
                SettingsDropdownItem(
                    label = "Tema",
                    selectedValue = selectedTheme.displayName,
                    expanded = themeExpanded,
                    onExpandedChange = { themeExpanded = it },
                    onDismiss = { themeExpanded = false },
                    options = Theme.entries.map { it.displayName },
                    onOptionSelected = { displayName ->
                        val theme = Theme.entries.first { it.displayName == displayName }
                        viewModel.updateTheme(theme)
                        themeExpanded = false
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sección: Servidor
            SettingsSection(
                title = "Servidor",
                icon = Icons.Filled.Cloud
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón de escanear QR
                    OutlinedButton(
                        onClick = { showQRScanner = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Escanear")
                    }

                    // Botón de compartir QR
                    OutlinedButton(
                        onClick = { showQRShare = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Compartir")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "O configura manualmente:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Dirección del servidor") },
                    placeholder = { Text("10.10.10.25") },
                    leadingIcon = { Icon(Icons.Filled.Computer, null) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Puerto") },
                    placeholder = { Text("8040") },
                    leadingIcon = { Icon(Icons.Filled.SettingsInputComponent, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingsSwitchItem(
                    title = "Usar HTTPS",
                    description = "Conexión segura cifrada",
                    checked = isHttps,
                    onCheckedChange = { isHttps = it },
                    icon = Icons.Filled.Lock
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        scope.launch {
                            val success = viewModel.testConnection()
                            val message = if (success) "✓ Conexión exitosa" else "✗ Error en la conexión"
                            snackbarHostState.showSnackbar(message)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Cable, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Probar conexión")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sección: Sincronización
            SettingsSection(
                title = "Sincronización",
                icon = Icons.Filled.Sync
            ) {
                SettingsSwitchItem(
                    title = "Sincronización automática",
                    description = "Sincronizar datos periódicamente",
                    checked = isSyncEnabled,
                    onCheckedChange = { isSyncEnabled = it },
                    icon = Icons.Filled.CloudSync
                )

                if (isSyncEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = interval,
                        onValueChange = { interval = it },
                        label = { Text("Intervalo de sincronización") },
                        placeholder = { Text("15") },
                        supportingText = { Text("Minutos entre cada sincronización") },
                        leadingIcon = { Icon(Icons.Filled.Timer, null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                SettingsSwitchItem(
                    title = "Botón de sincronización manual",
                    description = "Mostrar botón en pantalla principal",
                    checked = isShowSyncButton,
                    onCheckedChange = { isShowSyncButton = it },
                    icon = Icons.Filled.TouchApp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Botón Guardar
            Button(
                onClick = {
                    viewModel.saveConnectionDetails(address, port, isHttps)
                    viewModel.saveSyncSettings(isSyncEnabled, interval.toIntOrNull() ?: 15)
                    viewModel.saveShowSyncButton(isShowSyncButton)
                    scope.launch {
                        snackbarHostState.showSnackbar("✓ Ajustes guardados correctamente")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Guardar cambios")
            }
        }
    }
    // Dialogs
    if (showQRScanner) {
        QRScannerDialog(
            onDismiss = { showQRScanner = false },
            onConfigScanned = { config ->
                address = config.address
                port = config.port
                isHttps = config.useHttps
                scope.launch {
                    snackbarHostState.showSnackbar("✓ Configuración importada desde QR")
                }
            }
        )
    }

    if (showQRShare) {
        QRShareDialog(
            serverConfig = ServerConfig(
                address = address,
                port = port,
                useHttps = isHttps
            ),
            onDismiss = { showQRShare = false }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDropdownItem(
    label: String,
    selectedValue: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onOptionSelected(option) },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

enum class Theme(val displayName: String) {
    LIGHT("Claro"),
    DARK("Oscuro"),
    SYSTEM("Sistema")
}