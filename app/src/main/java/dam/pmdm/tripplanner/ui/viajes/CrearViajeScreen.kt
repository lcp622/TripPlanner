package dam.pmdm.tripplanner.ui.viajes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.platform.LocalLocale

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
    var fechaInicio by remember { mutableStateOf("") }
    var fechaFin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    val dateFormat = SimpleDateFormat("dd/MM/yyyy", LocalLocale.current.platformLocale)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Viaje") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
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
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = paisDestino,
                onValueChange = { paisDestino = it },
                label = { Text("País / Destino *") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = fechaInicio,
                onValueChange = { fechaInicio = it },
                label = { Text("Fecha inicio (dd/MM/yyyy) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = fechaFin,
                onValueChange = { fechaFin = it },
                label = { Text("Fecha fin (dd/MM/yyyy) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = presupuesto,
                onValueChange = { presupuesto = it },
                label = { Text("Presupuesto (€)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            if (error.isNotEmpty()) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    when {
                        nombre.isBlank() -> error = "El nombre es obligatorio"
                        paisDestino.isBlank() -> error = "El destino es obligatorio"
                        fechaInicio.isBlank() -> error = "La fecha de inicio es obligatoria"
                        fechaFin.isBlank() -> error = "La fecha de fin es obligatoria"
                        else -> {
                            try {
                                val inicio = dateFormat.parse(fechaInicio)!!.time
                                val fin = dateFormat.parse(fechaFin)!!.time
                                if (fin < inicio) {
                                    error = "La fecha de fin debe ser posterior a la de inicio"
                                } else {
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
                            } catch (e: Exception) {
                                error = "Formato de fecha incorrecto. Usa dd/MM/yyyy"
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Crear viaje")
            }
        }
    }
}