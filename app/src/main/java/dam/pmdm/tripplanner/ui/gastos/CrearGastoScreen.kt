package dam.pmdm.tripplanner.ui.gastos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

/**
 * Pantalla para crear un nuevo gasto en un viaje.
 * Carga los participantes del viaje desde Firestore al iniciarse
 * para calcular el reparto automático entre todos ellos.
 *
 * Realiza validaciones en tiempo real sobre el concepto e importe
 * antes de permitir crear el gasto.
 *
 * @param idViaje Identificador del viaje al que pertenece el gasto
 * @param viewModel ViewModel que gestiona la lógica de gastos
 * @param onGastoCreado Callback que se ejecuta al crear el gasto correctamente
 * @param onVolver Callback que navega a la pantalla anterior
 */
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

    /** Lista de participantes del viaje cargada desde Firestore para el reparto */
    var participantes by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    // Validaciones en tiempo real sobre los campos del formulario
    val errorConcepto = if (concepto.isNotBlank() && concepto.length < 3)
        "El concepto debe tener al menos 3 caracteres" else ""
    val errorImporte = if (importe.isBlank()) ""
    else if (importe.toDoubleOrNull() == null) "Introduce un número válido"
    else if (importe.toDouble() <= 0) "El importe debe ser mayor que 0"
    else ""

    val categorias = listOf("ALOJAMIENTO", "TRANSPORTE", "COMIDA", "OCIO", "OTROS")

    // Cargar participantes desde Firestore al entrar en la pantalla
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
            // Campo de concepto del gasto
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

            // Campo de importe con validación numérica
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

            // Selector de categoría del gasto
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
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
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

            // Campo de notas adicionales opcional
            OutlinedTextField(
                value = notas,
                onValueChange = { notas = it },
                label = { Text("Notas") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = tripTextFieldColors()
            )

            // Indicador informativo del número de personas en el reparto
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

            // Botón de creación con validación previa
            Button(
                onClick = {
                    // Validar campos antes de crear el gasto
                    when {
                        concepto.isBlank() -> errorGeneral = "El concepto es obligatorio"
                        importe.isBlank() -> errorGeneral = "El importe es obligatorio"
                        errorImporte.isNotEmpty() -> errorGeneral = errorImporte
                        else -> {
                            errorGeneral = ""
                            // Obtener datos del usuario autenticado como pagador
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