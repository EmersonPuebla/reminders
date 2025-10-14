package com.example.reminders.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDetailScreen(onBack: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var isNotifying by remember { mutableStateOf(false) }
    val selectedDate = remember { mutableStateOf<Long?>(null) }
    val showDatePicker = remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Crear Recordatorio") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = details,
                onValueChange = { details = it },
                label = { Text("Detalles") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Selector de fecha
            Button(onClick = { showDatePicker.value = true }) {
                Text(text = selectedDate.value?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it)) } ?: "Seleccionar fecha")
            }

            if (showDatePicker.value) {
                val datePickerState = rememberDatePickerState()
                DatePickerDialog(
                    onDismissRequest = { showDatePicker.value = false },
                    confirmButton = {
                        TextButton(onClick = {
                            selectedDate.value = datePickerState.selectedDateMillis
                            showDatePicker.value = false
                        }) {
                            Text("Aceptar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker.value = false }) {
                            Text("Cancelar")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = isNotifying, onCheckedChange = { isNotifying = it })
                Spacer(modifier = Modifier.width(8.dp))
                Text("Notificarme")
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = onBack) {
                    Text("Cancelar")
                }
                Button(onClick = onBack) {
                    Text("Guardar")
                }
            }
        }
    }
}