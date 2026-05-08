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
import dam.pmdm.tripplanner.data.local.entity.GastoEntity
import dam.pmdm.tripplanner.ui.theme.*

/**
 * Pantalla para editar un gasto existente en un viaje.
 * Los campos se inicializan con los datos actuales del gasto
 * para que el usuario solo modifique lo que necesite.
 *
 * Realiza las mismas validaciones en tiempo real que [CrearGastoScreen].
 * No permite modificar el reparto existente — solo los datos del gasto.
 *
 * @param gasto Entidad del gasto a editar con sus datos actuales
 * @param viewModel ViewModel que gestiona la lógica de gastos
 * @param onGastoActualizado Callback que se ejecuta al guardar los cambios
 * @param onVolver Callback que navega a la pantalla anterior
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarGastoScreen(
    gasto: GastoEntity,
    viewModel: GastoViewModel,
    onGastoActualizado: () -> Unit,
    onVolver: () -> Unit
) {
    // Inicializar campos con los datos actuales del gasto
    var concepto by remember { mutableStateOf(gasto.concepto) }
    var importe by remember { mutableStateOf(gasto.importe.toString()) }
    var notas by remember { mutableStateOf(gasto.notas ?: "") }
    var categoriaSeleccionada by remember { mutableStateOf(gasto.categoria) }
    var errorGeneral by remember { mutableStateOf("") }
    var expandedCategoria by remember { mutableStateOf(false) }

    // Validaciones en tiempo real sobre los campos del formulario
    val errorConcepto = if (concepto.isNotBlank() && concepto.length < 3)
        "El concepto debe tener al menos 3 caracteres" else ""
    val errorImporte = if (importe.isBlank()) ""
    else if (importe.toDoubleOrNull() == null) "Introduce un número válido"
    else if (importe.toDouble() <= 0) "El importe debe ser mayor que 0"
    else ""

    val categorias = listOf("ALOJAMIENTO", "TRANSPORTE", "COMIDA", "OCIO", "OTROS")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Editar Gasto",
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

            if (errorGeneral.isNotEmpty()) {
                Text(text = errorGeneral, color = MaterialTheme.colorScheme.error)
            }

            // Botón de guardar con validación previa
            Button(
                onClick = {
                    // Validar campos antes de actualizar el gasto
                    when {
                        concepto.isBlank() -> errorGeneral = "El concepto es obligatorio"
                        importe.isBlank() -> errorGeneral = "El importe es obligatorio"
                        errorImporte.isNotEmpty() -> errorGeneral = errorImporte
                        else -> {
                            errorGeneral = ""
                            // Actualizar solo los campos modificados manteniendo el resto
                            viewModel.actualizarGasto(
                                gasto.copy(
                                    concepto = concepto,
                                    importe = importe.toDouble(),
                                    categoria = categoriaSeleccionada,
                                    notas = notas.ifBlank { null }
                                )
                            )
                            onGastoActualizado()
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