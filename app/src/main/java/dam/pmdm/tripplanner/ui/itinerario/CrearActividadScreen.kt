package dam.pmdm.tripplanner.ui.itinerario

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dam.pmdm.tripplanner.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearActividadScreen(
    idViaje: String,
    viewModel: ActividadViewModel,
    onActividadCreada: () -> Unit,
    onVolver: () -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var lugar by remember { mutableStateOf("") }
    var errorGeneral by remember { mutableStateOf("") }

    var fechaSeleccionada by remember { mutableStateOf<LocalDate?>(null) }
    var horaInicioSeleccionada by remember { mutableStateOf<LocalTime?>(null) }
    var horaFinSeleccionada by remember { mutableStateOf<LocalTime?>(null) }

    val errorTitulo = if (titulo.isNotBlank() && titulo.length < 3)
        "El título debe tener al menos 3 caracteres" else ""
    val errorHoras = if (horaInicioSeleccionada != null && horaFinSeleccionada != null &&
        horaFinSeleccionada!!.isBefore(horaInicioSeleccionada))
        "La hora de fin debe ser posterior a la de inicio" else ""

    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    val mostrarPickerFecha = remember { mutableStateOf(false) }
    val mostrarPickerHoraInicio = remember { mutableStateOf(false) }
    val mostrarPickerHoraFin = remember { mutableStateOf(false) }

    val fechaPickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    val horaInicioPickerState = rememberTimePickerState(
        initialHour = 9,
        initialMinute = 0,
        is24Hour = true
    )
    val horaFinPickerState = rememberTimePickerState(
        initialHour = 10,
        initialMinute = 0,
        is24Hour = true
    )

    if (mostrarPickerFecha.value) {
        DatePickerDialog(
            onDismissRequest = { mostrarPickerFecha.value = false },
            confirmButton = {
                TextButton(onClick = {
                    fechaPickerState.selectedDateMillis?.let { millis ->
                        fechaSeleccionada = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    mostrarPickerFecha.value = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarPickerFecha.value = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = fechaPickerState)
        }
    }

    if (mostrarPickerHoraInicio.value) {
        AlertDialog(
            onDismissRequest = { mostrarPickerHoraInicio.value = false },
            confirmButton = {
                TextButton(onClick = {
                    horaInicioSeleccionada = LocalTime.of(horaInicioPickerState.hour, horaInicioPickerState.minute)
                    mostrarPickerHoraInicio.value = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarPickerHoraInicio.value = false }) { Text("Cancelar") }
            },
            text = { TimePicker(state = horaInicioPickerState) }
        )
    }

    if (mostrarPickerHoraFin.value) {
        AlertDialog(
            onDismissRequest = { mostrarPickerHoraFin.value = false },
            confirmButton = {
                TextButton(onClick = {
                    horaFinSeleccionada = LocalTime.of(horaFinPickerState.hour, horaFinPickerState.minute)
                    mostrarPickerHoraFin.value = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarPickerHoraFin.value = false }) { Text("Cancelar") }
            },
            text = { TimePicker(state = horaFinPickerState) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Nueva Actividad",
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
                value = titulo,
                onValueChange = { titulo = it },
                label = { Text("Título *") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = tripTextFieldColors(),
                isError = errorTitulo.isNotEmpty(),
                supportingText = if (errorTitulo.isNotEmpty()) {
                    { Text(errorTitulo, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                } else null
            )

            OutlinedTextField(
                value = fechaSeleccionada?.format(dateFormatter) ?: "",
                onValueChange = {},
                label = { Text("Fecha *") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { mostrarPickerFecha.value = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = TripBlue)
                    }
                },
                shape = MaterialTheme.shapes.medium,
                colors = tripTextFieldColors()
            )

            OutlinedTextField(
                value = horaInicioSeleccionada?.format(timeFormatter) ?: "",
                onValueChange = {},
                label = { Text("Hora inicio") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { mostrarPickerHoraInicio.value = true }) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = TripBlue)
                    }
                },
                shape = MaterialTheme.shapes.medium,
                colors = tripTextFieldColors()
            )

            OutlinedTextField(
                value = horaFinSeleccionada?.format(timeFormatter) ?: "",
                onValueChange = {},
                label = { Text("Hora fin") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { mostrarPickerHoraFin.value = true }) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = TripBlue)
                    }
                },
                shape = MaterialTheme.shapes.medium,
                colors = tripTextFieldColors(),
                isError = errorHoras.isNotEmpty(),
                supportingText = if (errorHoras.isNotEmpty()) {
                    { Text(errorHoras, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                } else null
            )

            OutlinedTextField(
                value = lugar,
                onValueChange = { lugar = it },
                label = { Text("Lugar") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = tripTextFieldColors()
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
                        titulo.isBlank() -> errorGeneral = "El título es obligatorio"
                        fechaSeleccionada == null -> errorGeneral = "La fecha es obligatoria"
                        errorHoras.isNotEmpty() -> errorGeneral = errorHoras
                        else -> {
                            errorGeneral = ""
                            val fechaLong = fechaSeleccionada!!
                                .atStartOfDay(ZoneId.systemDefault())
                                .toInstant().toEpochMilli()
                            viewModel.crearActividad(
                                idViaje = idViaje,
                                titulo = titulo,
                                descripcion = descripcion.ifBlank { null },
                                fecha = fechaLong,
                                horaInicio = horaInicioSeleccionada?.format(timeFormatter),
                                horaFin = horaFinSeleccionada?.format(timeFormatter),
                                lugar = lugar.ifBlank { null }
                            )
                            onActividadCreada()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = TripBlue)
            ) {
                Text("Crear actividad", fontWeight = FontWeight.Bold)
            }
        }
    }
}