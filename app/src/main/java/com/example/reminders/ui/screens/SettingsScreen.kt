package com.example.reminders.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reminders.ui.AppViewModelProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val selectedTheme by viewModel.theme.collectAsState()

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
        }
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
                        onClick = {
                            viewModel.updateTheme(theme)
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(theme.name)
                }
            }

            HorizontalDivider()

            Text(text = "Servidor", style = MaterialTheme.typography.titleLarge)

            Text("Dirección")
            OutlinedTextField(
                value = "",
                onValueChange = { /* TODO */ },
                placeholder = {
                    Text("10.10.10.25")
                }
            )

            Text("Puerto")
            OutlinedTextField(
                value = "",
                onValueChange = { /* TODO */ },
                placeholder = {
                    Text("8040")
                }
            )
        }
    }
}

enum class Theme {
    LIGHT,
    DARK,
    SYSTEM
}
