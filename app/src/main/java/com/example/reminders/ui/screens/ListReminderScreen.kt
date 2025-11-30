package com.example.reminders.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reminders.data.Reminder
import com.example.reminders.ui.AppViewModelProvider
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReminderListScreen(
    onSettingsClick: () -> Unit,
    viewModel: ReminderListViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val reminderListUiState by viewModel.reminderListUiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val filteredReminders = reminderListUiState.itemList.filter { it.title.contains(searchQuery, ignoreCase = true) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedItems by remember { mutableStateOf(setOf<Int>()) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    var syncMessage by remember { mutableStateOf("") }
    var showSnackbar by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showReadSheet by remember { mutableStateOf(false) }
    val readSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedReminder by remember { mutableStateOf<Reminder?>(null) }
    var reorderMode by remember { mutableStateOf(false) }
    var reorderedList by remember { mutableStateOf(filteredReminders) }
    val coroutineScope = rememberCoroutineScope()

    // Actualizar la lista reordenada cuando cambia filteredReminders
    LaunchedEffect(filteredReminders) {
        if (!reorderMode) {
            reorderedList = filteredReminders
        }
    }

    // Mostrar EditReminderView como Dialog directo
    if (showEditDialog) {
        EditReminderView(onBack = { showEditDialog = false })
    }

    if (showReadSheet) {
        ModalBottomSheet(
            onDismissRequest = { showReadSheet = false },
            sheetState = readSheetState
        ) {
            selectedReminder?.let {
                ReadReminderView(reminder = it, onEditClick = {
                    showReadSheet = false
                    showEditDialog = true
                })
            }
        }
    }

    LaunchedEffect(showSnackbar) {
        if (showSnackbar) {
            snackbarHostState.showSnackbar(syncMessage)
            showSnackbar = false
        }
    }

    val userPreferencesRepository = (LocalContext.current.applicationContext as? com.example.reminders.RemindersApplication)?.container?.userPreferencesRepository
    val showSyncButton by (userPreferencesRepository?.showSyncButton?.collectAsState(initial = false) ?: remember { mutableStateOf(false) })

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
            when {
                selectionMode -> {
                    TopAppBar(
                        title = { Text("${selectedItems.size} seleccionados") },
                        navigationIcon = {
                            IconButton(onClick = {
                                selectionMode = false
                                selectedItems = emptySet()
                            }) {
                                Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Atrás")
                            }
                        }
                    )
                }
                reorderMode -> {
                    TopAppBar(
                        title = { Text("Reordenar") },
                        navigationIcon = {
                            IconButton(onClick = {
                                reorderMode = false
                                reorderedList = filteredReminders
                            }) {
                                Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Cancelar")
                            }
                        },
                        actions = {
                            TextButton(onClick = {
                                coroutineScope.launch {
                                    viewModel.updateRemindersOrder(reorderedList.map { it.id })
                                    reorderMode = false
                                }
                            }) {
                                Text("Guardar")
                            }
                        }
                    )
                }
                else -> {
                    TopAppBar(
                        title = { Text("Reminders") },
                        actions = {
                            IconButton(onClick = { reorderMode = true }) {
                                Icon(Icons.Filled.SwapVert, contentDescription = "Reordenar")
                            }
                            IconButton(onClick = onSettingsClick) {
                                Icon(Icons.Filled.Settings, contentDescription = "Ajustes")
                            }
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            when {
                selectionMode -> {
                    FloatingActionButton(onClick = { showDeleteConfirmation = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                    }
                }
                !reorderMode -> {
                    FloatingActionButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Añadir Recordatorio")
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding)
        ) {
            if (!reorderMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onSearch = { },
                        active = false,
                        onActiveChange = { },
                        placeholder = { Text("Buscar") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {}

                    if (showSyncButton) {
                        IconButton(
                            onClick = {
                                isSyncing = true
                                viewModel.syncRemindersAndFetchMissingAsync { result ->
                                    syncMessage = if (result.success) {
                                        "Sincronizado: ${result.newRemindersCount} nuevos, ${result.updatedRemindersCount} actualizados"
                                    } else {
                                        "Error: ${result.message}"
                                    }
                                    showSnackbar = true
                                    isSyncing = false
                                }
                            },
                            modifier = Modifier.align(Alignment.CenterVertically),
                            enabled = !isSyncing
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            } else {
                                Icon(Icons.Filled.Sync, contentDescription = "Sincronizar")
                            }
                        }
                    }
                }
            }

            if (reorderedList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No se encontraron resultados" else "No tienes reminders",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            } else {
                if (reorderMode) {
                    val state = rememberReorderableLazyListState(
                        onMove = { from, to ->
                            reorderedList = reorderedList.toMutableList().apply {
                                add(to.index, removeAt(from.index))
                            }
                        }
                    )

                    LazyColumn(
                        state = state.listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .reorderable(state)
                    ) {
                        itemsIndexed(reorderedList, key = { _, item -> item.id }) { index, reminder ->
                            ReorderableItem(state, key = reminder.id) { isDragging ->
                                ReminderReorderItem(
                                    reminder = reminder,
                                    isDragging = isDragging,
                                    modifier = Modifier.detectReorderAfterLongPress(state)
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(reorderedList, key = { it.id }) { reminder ->
                            ReminderListItem(
                                reminder = reminder,
                                isSelected = selectedItems.contains(reminder.id),
                                onClick = {
                                    if (selectionMode) {
                                        toggleSelection(reminder.id)
                                    } else {
                                        selectedReminder = reminder
                                        showReadSheet = true
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReminderListItem(
    reminder: Reminder,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(text = reminder.title, style = MaterialTheme.typography.titleMedium) },
        supportingContent = {
            Text(
                text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(reminder.date)),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = ListItemDefaults.colors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
        )
    )
}

@Composable
fun ReminderReorderItem(
    reminder: Reminder,
    isDragging: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragging) 8.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.DragHandle,
                contentDescription = "Arrastrar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(reminder.date)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}