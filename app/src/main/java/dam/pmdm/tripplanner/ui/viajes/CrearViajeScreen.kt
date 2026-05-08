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
import com.google.firebase.auth.FirebaseAuth
import dam.pmdm.tripplanner.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Pantalla para crear un nuevo viaje.
 * Incluye campos para nombre, destino, fechas, presupuesto y descripción.
 *
 * Usa [DatePickerDialog] nativo de Material3 para seleccionar fechas,
 * inicializado en la fecha actual para inicio y en 7 días para fin.
 *
 * Realiza validaciones en tiempo real sobre nombre, destino y presupuesto,
 * y valida la coherencia de fechas antes de permitir crear el viaje.
 * El propietario se obtiene automáticamente del usuario autenticado en Firebase.
 *
 * @param viewModel ViewModel que gestiona la creación del viaje
 * @param onViajeCreado Callback que se ejecuta al crear el viaje correctamente
 * @param onVolver Callback que navega a la pantalla anterior
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearViajeScreen(
    viewModel: ViajeViewModel,
    onViajeCreado: () -> Unit,
    onVolver: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var paisDestino by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var presupuesto by remember { mutableStateOf("") }

    // Validaciones en tiempo real — vacío no es error hasta que el usuario escribe
    val errorNombre = if (nombre.isBlank() && nombre.isNotEmpty().not()) ""
    else if (nombre.isNotBlank() && nombre.length < 3) "El nombre debe tener al menos 3 caracteres"
    else ""
    val errorDestino = if (paisDestino.isBlank() && paisDestino.isNotEmpty().not()) ""
    else if (paisDestino.isNotBlank() && paisDestino.length < 2) "El destino debe tener al menos 2 caracteres"
    else ""
    val errorPresupuesto = if (presupuesto.isBlank()) ""
    else if (presupuesto.toDoubleOrNull() == null) "Introduce un número válido"
    else if (presupuesto.toDouble() < 0) "El presupuesto no puede ser negativo"
    else ""

    /** Fechas seleccionadas — null hasta que el usuario las elige en el DatePicker */
    var fechaInicioSeleccionada by remember { mutableStateOf<LocalDate?>(null) }
    var fechaFinSeleccionada by remember { mutableStateOf<LocalDate?>(null) }
    val errorFechas = if (fechaInicioSeleccionada != null && fechaFinSeleccionada != null &&
        fechaFinSeleccionada!!.isBefore(fechaInicioSeleccionada))
        "La fecha de fin debe ser posterior a la de inicio" else ""

    var errorGeneral by remember { mutableStateOf("") }

    /** Controla la visibilidad del DatePickerDialog de fecha de inicio */
    val mostrarPickerInicio = remember { mutableStateOf(false) }

    /** Controla la visibilidad del DatePickerDialog de fecha de fin */
    val mostrarPickerFin = remember { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    // Inicializar el DatePicker de inicio en la fecha actual
    val fechaInicioPickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    // Inicializar el DatePicker de fin 7 días después de hoy
    val fechaFinPickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L
    )

    // DatePickerDialog nativo de Material3 para la fecha de inicio
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

    // DatePickerDialog nativo de Material3 para la fecha de fin
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
                        "Nuevo Viaje",
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
            // Campo de nombre del viaje
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

            // Campo de país o destino
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

            // Campo de fecha de inicio — abre el DatePickerDialog al pulsar el icono
            OutlinedTextField(
                value = fechaInicioSeleccionada?.format(dateFormatter) ?: "",
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

            // Campo de fecha de fin — abre el DatePickerDialog al pulsar el icono
            OutlinedTextField(
                value = fechaFinSeleccionada?.format(dateFormatter) ?: "",
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

            // Campo de presupuesto con validación numérica
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

            // Campo de descripción opcional
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

            // Botón de creación con validación previa de todos los campos
            Button(
                onClick = {
                    when {
                        nombre.isBlank() -> errorGeneral = "El nombre es obligatorio"
                        paisDestino.isBlank() -> errorGeneral = "El destino es obligatorio"
                        fechaInicioSeleccionada == null -> errorGeneral = "La fecha de inicio es obligatoria"
                        fechaFinSeleccionada == null -> errorGeneral = "La fecha de fin es obligatoria"
                        errorFechas.isNotEmpty() -> errorGeneral = errorFechas
                        errorPresupuesto.isNotEmpty() -> errorGeneral = errorPresupuesto
                        else -> {
                            errorGeneral = ""
                            // Convertir LocalDate a milisegundos para almacenar en la entidad
                            val inicio = fechaInicioSeleccionada!!
                                .atStartOfDay(ZoneId.systemDefault())
                                .toInstant().toEpochMilli()
                            val fin = fechaFinSeleccionada!!
                                .atStartOfDay(ZoneId.systemDefault())
                                .toInstant().toEpochMilli()
                            // Obtener el UID del propietario desde Firebase Auth
                            val idUsuario = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                            viewModel.crearViaje(
                                nombre = nombre,
                                paisDestino = paisDestino,
                                fechaInicio = inicio,
                                fechaFin = fin,
                                descripcion = descripcion.ifBlank { null },
                                presupuesto = presupuesto.toDoubleOrNull() ?: 0.0,
                                idPropietario = idUsuario
                            )
                            onViajeCreado()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = TripBlue)
            ) {
                Text("Crear viaje", fontWeight = FontWeight.Bold)
            }
        }
    }
}