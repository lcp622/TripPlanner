package dam.pmdm.tripplanner.ui.viajes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dam.pmdm.tripplanner.data.local.entity.ViajeEntity
import dam.pmdm.tripplanner.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarViajeScreen(
    viaje: ViajeEntity,
    viewModel: ViajeViewModel,
    onViajeActualizado: () -> Unit,
    onVolver: () -> Unit
) {
    var nombre by remember { mutableStateOf(viaje.nombre) }
    var paisDestino by remember { mutableStateOf(viaje.paisDestino) }
    var descripcion by remember { mutableStateOf(viaje.descripcion ?: "") }
    var presupuesto by remember { mutableStateOf(viaje.presupuestoTotal.toString()) }
    var errorGeneral by remember { mutableStateOf("") }

    val errorNombre = if (nombre.isNotBlank() && nombre.length < 3)
        "El nombre debe tener al menos 3 caracteres" else ""
    val errorDestino = if (paisDestino.isNotBlank() && paisDestino.length < 2)
        "El destino debe tener al menos 2 caracteres" else ""
    val errorPresupuesto = if (presupuesto.isBlank()) ""
    else if (presupuesto.toDoubleOrNull() == null) "Introduce un número válido"
    else if (presupuesto.toDouble() < 0) "El presupuesto no puede ser negativo"
    else ""

    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    var fechaInicioSeleccionada by remember {
        mutableStateOf(
            Instant.ofEpochMilli(viaje.fechaInicio).atZone(ZoneId.systemDefault()).toLocalDate()
        )
    }
    var fechaFinSeleccionada by remember {
        mutableStateOf(
            Instant.ofEpochMilli(viaje.fechaFin).atZone(ZoneId.systemDefault()).toLocalDate()
        )
    }

    val errorFechas = if (fechaFinSeleccionada.isBefore(fechaInicioSeleccionada))
        "La fecha de fin debe ser posterior a la de inicio" else ""

    val mostrarPickerInicio = remember { mutableStateOf(false) }
    val mostrarPickerFin = remember { mutableStateOf(false) }

    val fechaInicioPickerState = rememberDatePickerState(
        initialSelectedDateMillis = viaje.fechaInicio
    )
    val fechaFinPickerState = rememberDatePickerState(
        initialSelectedDateMillis = viaje.fechaFin
    )

    if (mostrarPickerInicio.value) {
        DatePickerDialog(
            onDismissRequest = { mostrarPickerInicio.value = false },
            confirmButton = {
                TextButton(onClick = {
                    fechaInicioPickerState.selectedDateMillis?.let { millis ->
                        fechaInicioSeleccionada = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    mostrarPickerInicio.value = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarPickerInicio.value = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = fechaInicioPickerState)
        }
    }

    if (mostrarPickerFin.value) {
        DatePickerDialog(
            onDismissRequest = { mostrarPickerFin.value = false },
            confirmButton = {
                TextButton(onClick = {
                    fechaFinPickerState.selectedDateMillis?.let { millis ->
                        fechaFinSeleccionada = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    mostrarPickerFin.value = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarPickerFin.value = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = fechaFinPickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Editar Viaje",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre del viaje *") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = tripTextFieldColors(),
                isError = errorNombre.isNotEmpty(),
                supportingText = if (errorNombre.isNotEmpty()) {
                    { Text(errorNombre, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                } else null
            )

            OutlinedTextField(
                value = paisDestino,
                onValueChange = { paisDestino = it },
                label = { Text("País / Destino *") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = tripTextFieldColors(),
                isError = errorDestino.isNotEmpty(),
                supportingText = if (errorDestino.isNotEmpty()) {
                    { Text(errorDestino, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                } else null
            )

            OutlinedTextField(
                value = fechaInicioSeleccionada.format(dateFormatter),
                onValueChange = {},
                label = { Text("Fecha inicio *") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { mostrarPickerInicio.value = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = TripBlue)
                    }
                },
                shape = MaterialTheme.shapes.medium,
                colors = tripTextFieldColors()
            )

            OutlinedTextField(
                value = fechaFinSeleccionada.format(dateFormatter),
                onValueChange = {},
                label = { Text("Fecha fin *") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { mostrarPickerFin.value = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = TripBlue)
                    }
                },
                shape = MaterialTheme.shapes.medium,
                colors = tripTextFieldColors(),
                isError = errorFechas.isNotEmpty(),
                supportingText = if (errorFechas.isNotEmpty()) {
                    { Text(errorFechas, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                } else null
            )

            OutlinedTextField(
                value = presupuesto,
                onValueChange = { presupuesto = it },
                label = { Text("Presupuesto (€)") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = tripTextFieldColors(),
                isError = errorPresupuesto.isNotEmpty(),
                supportingText = if (errorPresupuesto.isNotEmpty()) {
                    { Text(errorPresupuesto, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                } else null
            )

            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = tripTextFieldColors()
            )

            if (errorGeneral.isNotEmpty()) {
                Text(text = errorGeneral, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    when {
                        nombre.isBlank() -> errorGeneral = "El nombre es obligatorio"
                        paisDestino.isBlank() -> errorGeneral = "El destino es obligatorio"
                        errorFechas.isNotEmpty() -> errorGeneral = errorFechas
                        errorPresupuesto.isNotEmpty() -> errorGeneral = errorPresupuesto
                        else -> {
                            errorGeneral = ""
                            val inicio = fechaInicioSeleccionada
                                .atStartOfDay(ZoneId.systemDefault())
                                .toInstant().toEpochMilli()
                            val fin = fechaFinSeleccionada
                                .atStartOfDay(ZoneId.systemDefault())
                                .toInstant().toEpochMilli()
                            viewModel.actualizarViaje(
                                viaje.copy(
                                    nombre = nombre,
                                    paisDestino = paisDestino,
                                    fechaInicio = inicio,
                                    fechaFin = fin,
                                    descripcion = descripcion.ifBlank { null },
                                    presupuestoTotal = presupuesto.toDoubleOrNull() ?: 0.0
                                )
                            )
                            onViajeActualizado()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = TripBlue)
            ) {
                Text("Guardar cambios", fontWeight = FontWeight.Bold)
            }
        }
    }
}