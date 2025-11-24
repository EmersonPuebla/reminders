package com.example.reminders.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reminders.data.Reminder
import com.example.reminders.ui.AppViewModelProvider

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReminderListScreen(
    onAddReminder: () -> Unit,
    onItemClick: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: ReminderListViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val reminderListUiState by viewModel.reminderListUiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val filteredReminders = reminderListUiState.itemList.filter { it.title.contains(searchQuery, ignoreCase = true) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedItems by remember { mutableStateOf(setOf<Int>()) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    fun toggleSelection(reminderId: Int) {
        val newSelectedItems = selectedItems.toMutableSet()
        if (newSelectedItems.contains(reminderId)) {
            newSelectedItems.remove(reminderId)
        } else {
            newSelectedItems.add(reminderId)
        }
        selectedItems = newSelectedItems
        if (selectedItems.isEmpty()) {
            selectionMode = false
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Estás seguro de que deseas eliminar los recordatorios seleccionados?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteReminders(selectedItems.toList())
                    selectedItems = emptySet()
                    selectionMode = false
                    showDeleteConfirmation = false
                }) {
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

    Scaffold(
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    title = { Text("${selectedItems.size} seleccionados") },
                    navigationIcon = {
                        IconButton(onClick = {
                            selectionMode = false
                            selectedItems = emptySet()
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Reminders") },
                    actions = {
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Filled.Settings, contentDescription = "Ajustes")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (selectionMode) {
                FloatingActionButton(onClick = { showDeleteConfirmation = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                }
            } else {
                FloatingActionButton(onClick = onAddReminder) {
                    Icon(Icons.Filled.Add, contentDescription = "Añadir Recordatorio")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            if (filteredReminders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No tienes reminders", style = MaterialTheme.typography.titleLarge)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredReminders) { reminder ->
                        ReminderListItem(
                            reminder = reminder,
                            isSelected = selectedItems.contains(reminder.id),
                            onClick = {
                                if (selectionMode) {
                                    toggleSelection(reminder.id)
                                } else {
                                    onItemClick(reminder.id)
                                }
                            },
                            onLongClick = {
                                if (!selectionMode) {
                                    selectionMode = true
                                    toggleSelection(reminder.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReminderListItem(
    reminder: Reminder,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = reminder.title, style = MaterialTheme.typography.titleMedium)
        }
    }
}
