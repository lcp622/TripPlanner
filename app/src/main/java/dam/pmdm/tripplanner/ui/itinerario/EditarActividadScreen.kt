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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dam.pmdm.tripplanner.data.local.entity.ActividadEntity
import dam.pmdm.tripplanner.ui.theme.*
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Pantalla para la edición de una actividad ya existente en el itinerario.
 *
 * Carga los datos iniciales de la [actividad] proporcionada y permite al usuario
 * modificar sus campos. Incluye:
 * - Validación de longitud de título.
 * - Validación lógica de horarios (la hora de fin no puede ser previa a la de inicio).
 * - Selectores de fecha y hora mediante diálogos nativos de Material3.
 * - Actualización reactiva del estado mediante el [ActividadViewModel].
 *
 * @param actividad La entidad de la actividad que se desea modificar.
 * @param viewModel ViewModel que gestiona la lógica de actualización en la base de datos.
 * @param onActividadActualizada Callback invocado tras guardar los cambios con éxito.
 * @param onVolver Callback invocado para regresar a la pantalla anterior sin guardar cambios.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarActividadScreen(
    actividad: ActividadEntity,
    viewModel: ActividadViewModel,
    onActividadActualizada: () -> Unit,
    onVolver: () -> Unit
) {
    // Inicialización del estado con los valores actuales de la actividad
    var titulo by remember { mutableStateOf(actividad.titulo) }
    var descripcion by remember { mutableStateOf(actividad.descripcion ?: "") }
    var lugar by remember { mutableStateOf(actividad.lugar ?: "") }
    var errorGeneral by remember { mutableStateOf("") }

    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    // Conversión de datos de la entidad a objetos de tiempo de Java 8
    var fechaSeleccionada by remember {
        mutableStateOf(
            Instant.ofEpochMilli(actividad.fecha).atZone(ZoneId.systemDefault()).toLocalDate()
        )
    }
    var horaInicioSeleccionada by remember {
        mutableStateOf(actividad.horaInicio?.let { LocalTime.parse(it) })
    }
    var horaFinSeleccionada by remember {
        mutableStateOf(actividad.horaFin?.let { LocalTime.parse(it) })
    }

    // Lógica de validación en tiempo real
    val errorTitulo = if (titulo.isNotBlank() && titulo.length < 3)
        "El título debe tener al menos 3 caracteres" else ""
    val errorHoras = if (horaInicioSeleccionada != null && horaFinSeleccionada != null &&
        horaFinSeleccionada!!.isBefore(horaInicioSeleccionada))
        "La hora de fin debe ser posterior a la de inicio" else ""

    // Control de visibilidad de los selectores (Pickers)
    val mostrarPickerFecha = remember { mutableStateOf(false) }
    val mostrarPickerHoraInicio = remember { mutableStateOf(false) }
    val mostrarPickerHoraFin = remember { mutableStateOf(false) }

    // Estados de los componentes Picker de Material3
    val fechaPickerState = rememberDatePickerState(
        initialSelectedDateMillis = actividad.fecha
    )
    val horaInicioPickerState = rememberTimePickerState(
        initialHour = horaInicioSeleccionada?.hour ?: 9,
        initialMinute = horaInicioSeleccionada?.minute ?: 0,
        is24Hour = true
    )
    val horaFinPickerState = rememberTimePickerState(
        initialHour = horaFinSeleccionada?.hour ?: 10,
        initialMinute = horaFinSeleccionada?.minute ?: 0,
        is24Hour = true
    )

    // Diálogo de Selección de Fecha
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

    // Diálogo de Selección de Hora de Inicio
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

    // Diálogo de Selección de Hora de Fin
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
                        "Editar Actividad",
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
            // Campo: Título con validación
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

            // Campo: Fecha
            OutlinedTextField(
                value = fechaSeleccionada.format(dateFormatter),
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

            // Campo: Hora Inicio
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

            // Campo: Hora Fin con validación lógica
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

            // Campo: Lugar
            OutlinedTextField(
                value = lugar,
                onValueChange = { lugar = it },
                label = { Text("Lugar") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = tripTextFieldColors()
            )

            // Campo: Descripción
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = tripTextFieldColors()
            )

            // Mostrar error general si existe
            if (errorGeneral.isNotEmpty()) {
                Text(text = errorGeneral, color = MaterialTheme.colorScheme.error)
            }

            // Botón para persistir los cambios
            Button(
                onClick = {
                    when {
                        titulo.isBlank() -> errorGeneral = "El título es obligatorio"
                        errorTitulo.isNotEmpty() -> errorGeneral = errorTitulo
                        errorHoras.isNotEmpty() -> errorGeneral = errorHoras
                        else -> {
                            errorGeneral = ""
                            val fechaLong = fechaSeleccionada
                                .atStartOfDay(ZoneId.systemDefault())
                                .toInstant().toEpochMilli()

                            // Creamos una copia de la entidad con los valores actualizados
                            viewModel.actualizarActividad(
                                actividad.copy(
                                    titulo = titulo,
                                    descripcion = descripcion.ifBlank { null },
                                    fecha = fechaLong,
                                    horaInicio = horaInicioSeleccionada?.format(timeFormatter),
                                    horaFin = horaFinSeleccionada?.format(timeFormatter),
                                    lugar = lugar.ifBlank { null }
                                )
                            )
                            onActividadActualizada()
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