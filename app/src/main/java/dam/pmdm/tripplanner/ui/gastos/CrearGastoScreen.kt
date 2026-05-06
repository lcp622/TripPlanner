package dam.pmdm.tripplanner.ui.gastos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dam.pmdm.tripplanner.ui.theme.*
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearGastoScreen(
    idViaje: String,
    viewModel: GastoViewModel,
    onGastoCreado: () -> Unit,
    onVolver: () -> Unit
) {
    var concepto by remember { mutableStateOf("") }
    var importe by remember { mutableStateOf("") }
    var notas by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf("OTROS") }
    var errorGeneral by remember { mutableStateOf("") }
    var expandedCategoria by remember { mutableStateOf(false) }
    var participantes by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    // Validaciones en tiempo real
    val errorConcepto = if (concepto.isNotBlank() && concepto.length < 3)
        "El concepto debe tener al menos 3 caracteres" else ""
    val errorImporte = if (importe.isBlank()) ""
    else if (importe.toDoubleOrNull() == null) "Introduce un número válido"
    else if (importe.toDouble() <= 0) "El importe debe ser mayor que 0"
    else ""

    val categorias = listOf("ALOJAMIENTO", "TRANSPORTE", "COMIDA", "OCIO", "OTROS")

    LaunchedEffect(idViaje) {
        val snapshot = FirebaseFirestore.getInstance()
            .collection("viajes").document(idViaje)
            .collection("participantes")
            .get().await()
        participantes = snapshot.documents.mapNotNull { it.data }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Nuevo Gasto",
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
                value = concepto,
                onValueChange = { concepto = it },
                label = { Text("Concepto *") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = tripTextFieldColors(),
                isError = errorConcepto.isNotEmpty(),
                supportingText = if (errorConcepto.isNotEmpty()) {
                    { Text(errorConcepto, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                } else null
            )

            OutlinedTextField(
                value = importe,
                onValueChange = { importe = it },
                label = { Text("Importe (€) *") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = tripTextFieldColors(),
                isError = errorImporte.isNotEmpty(),
                supportingText = if (errorImporte.isNotEmpty()) {
                    { Text(errorImporte, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                } else null
            )

            ExposedDropdownMenuBox(
                expanded = expandedCategoria,
                onExpandedChange = { expandedCategoria = it }
            ) {
                OutlinedTextField(
                    value = categoriaSeleccionada,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoría") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoria) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = MaterialTheme.shapes.medium,
                    colors = tripTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = expandedCategoria,
                    onDismissRequest = { expandedCategoria = false }
                ) {
                    categorias.forEach { categoria ->
                        DropdownMenuItem(
                            text = { Text(categoria) },
                            onClick = {
                                categoriaSeleccionada = categoria
                                expandedCategoria = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = notas,
                onValueChange = { notas = it },
                label = { Text("Notas") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = tripTextFieldColors()
            )

            if (participantes.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = TripBlue.copy(alpha = 0.1f)
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "💡 El gasto se repartirá entre ${participantes.size} personas",
                        modifier = Modifier.padding(12.dp),
                        fontSize = 13.sp,
                        color = TripBlue
                    )
                }
            }

            if (errorGeneral.isNotEmpty()) {
                Text(text = errorGeneral, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    when {
                        concepto.isBlank() -> errorGeneral = "El concepto es obligatorio"
                        importe.isBlank() -> errorGeneral = "El importe es obligatorio"
                        errorImporte.isNotEmpty() -> errorGeneral = errorImporte
                        else -> {
                            errorGeneral = ""
                            val user = FirebaseAuth.getInstance().currentUser
                            val idUsuario = user?.uid ?: ""
                            val nombreUsuario = user?.displayName
                                ?: user?.email?.substringBefore("@")
                                ?: "Usuario"
                            viewModel.crearGastoConReparto(
                                idViaje = idViaje,
                                idPagador = idUsuario,
                                nombrePagador = nombreUsuario,
                                concepto = concepto,
                                importe = importe.toDouble(),
                                categoria = categoriaSeleccionada,
                                notas = notas.ifBlank { null },
                                participantes = participantes
                            )
                            onGastoCreado()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = TripBlue)
            ) {
                Text("Añadir gasto", fontWeight = FontWeight.Bold)
            }
        }
    }
}