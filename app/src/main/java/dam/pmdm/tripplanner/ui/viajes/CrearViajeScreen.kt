package dam.pmdm.tripplanner.ui.viajes

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.datetime.date.datepicker
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import dam.pmdm.tripplanner.ui.theme.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
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
    var error by remember { mutableStateOf("") }

    var fechaInicioSeleccionada by remember { mutableStateOf<LocalDate?>(null) }
    var fechaFinSeleccionada by remember { mutableStateOf<LocalDate?>(null) }

    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val fechaInicioDialogState = rememberMaterialDialogState()
    val fechaFinDialogState = rememberMaterialDialogState()

    MaterialDialog(
        dialogState = fechaInicioDialogState,
        buttons = {
            positiveButton("OK")
            negativeButton("Cancelar")
        }
    ) {
        datepicker(
            initialDate = LocalDate.now(),
            title = "Fecha de inicio"
        ) { fecha -> fechaInicioSeleccionada = fecha }
    }

    MaterialDialog(
        dialogState = fechaFinDialogState,
        buttons = {
            positiveButton("OK")
            negativeButton("Cancelar")
        }
    ) {
        datepicker(
            initialDate = LocalDate.now().plusDays(7),
            title = "Fecha de fin"
        ) { fecha -> fechaFinSeleccionada = fecha }
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
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre del viaje *") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = tripTextFieldColors()
            )

            OutlinedTextField(
                value = paisDestino,
                onValueChange = { paisDestino = it },
                label = { Text("País / Destino *") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = tripTextFieldColors()
            )

            OutlinedTextField(
                value = fechaInicioSeleccionada?.format(dateFormatter) ?: "",
                onValueChange = {},
                label = { Text("Fecha inicio *") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { fechaInicioDialogState.show() }) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = TripBlue)
                    }
                },
                shape = MaterialTheme.shapes.medium,
                colors = tripTextFieldColors()
            )

            OutlinedTextField(
                value = fechaFinSeleccionada?.format(dateFormatter) ?: "",
                onValueChange = {},
                label = { Text("Fecha fin *") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { fechaFinDialogState.show() }) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = TripBlue)
                    }
                },
                shape = MaterialTheme.shapes.medium,
                colors = tripTextFieldColors()
            )

            OutlinedTextField(
                value = presupuesto,
                onValueChange = { presupuesto = it },
                label = { Text("Presupuesto (€)") },
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

            if (error.isNotEmpty()) {
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    when {
                        nombre.isBlank() -> error = "El nombre es obligatorio"
                        paisDestino.isBlank() -> error = "El destino es obligatorio"
                        fechaInicioSeleccionada == null -> error = "La fecha de inicio es obligatoria"
                        fechaFinSeleccionada == null -> error = "La fecha de fin es obligatoria"
                        fechaFinSeleccionada!!.isBefore(fechaInicioSeleccionada) -> error = "La fecha de fin debe ser posterior"
                        else -> {
                            val inicio = fechaInicioSeleccionada!!
                                .atStartOfDay(ZoneId.systemDefault())
                                .toInstant().toEpochMilli()
                            val fin = fechaFinSeleccionada!!
                                .atStartOfDay(ZoneId.systemDefault())
                                .toInstant().toEpochMilli()
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