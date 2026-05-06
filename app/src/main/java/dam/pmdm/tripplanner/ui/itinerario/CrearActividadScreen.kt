package dam.pmdm.tripplanner.ui.itinerario

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.datetime.date.datepicker
import com.vanpra.composematerialdialogs.datetime.time.timepicker
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import dam.pmdm.tripplanner.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
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

    // Validaciones en tiempo real
    val errorTitulo = if (titulo.isNotBlank() && titulo.length < 3)
        "El título debe tener al menos 3 caracteres" else ""
    val errorHoras = if (horaInicioSeleccionada != null && horaFinSeleccionada != null &&
        horaFinSeleccionada!!.isBefore(horaInicioSeleccionada))
        "La hora de fin debe ser posterior a la de inicio" else ""

    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    val fechaDialogState = rememberMaterialDialogState()
    val horaInicioDialogState = rememberMaterialDialogState()
    val horaFinDialogState = rememberMaterialDialogState()

    MaterialDialog(
        dialogState = fechaDialogState,
        buttons = {
            positiveButton("OK")
            negativeButton("Cancelar")
        }
    ) {
        datepicker(
            initialDate = LocalDate.now(),
            title = "Selecciona la fecha"
        ) { fecha -> fechaSeleccionada = fecha }
    }

    MaterialDialog(
        dialogState = horaInicioDialogState,
        buttons = {
            positiveButton("OK")
            negativeButton("Cancelar")
        }
    ) {
        timepicker(
            initialTime = LocalTime.of(9, 0),
            title = "Hora de inicio",
            is24HourClock = true
        ) { hora -> horaInicioSeleccionada = hora }
    }

    MaterialDialog(
        dialogState = horaFinDialogState,
        buttons = {
            positiveButton("OK")
            negativeButton("Cancelar")
        }
    ) {
        timepicker(
            initialTime = LocalTime.of(10, 0),
            title = "Hora de fin",
            is24HourClock = true
        ) { hora -> horaFinSeleccionada = hora }
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
                            Icons.Default.ArrowBack,
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
                    IconButton(onClick = { fechaDialogState.show() }) {
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
                    IconButton(onClick = { horaInicioDialogState.show() }) {
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
                    IconButton(onClick = { horaFinDialogState.show() }) {
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