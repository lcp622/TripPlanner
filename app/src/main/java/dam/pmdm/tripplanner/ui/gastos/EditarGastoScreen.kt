package dam.pmdm.tripplanner.ui.gastos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dam.pmdm.tripplanner.data.local.entity.GastoEntity
import dam.pmdm.tripplanner.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarGastoScreen(
    gasto: GastoEntity,
    viewModel: GastoViewModel,
    onGastoActualizado: () -> Unit,
    onVolver: () -> Unit
) {
    var concepto by remember { mutableStateOf(gasto.concepto) }
    var importe by remember { mutableStateOf(gasto.importe.toString()) }
    var notas by remember { mutableStateOf(gasto.notas ?: "") }
    var categoriaSeleccionada by remember { mutableStateOf(gasto.categoria) }
    var error by remember { mutableStateOf("") }
    var expandedCategoria by remember { mutableStateOf(false) }

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
            OutlinedTextField(
                value = concepto,
                onValueChange = { concepto = it },
                label = { Text("Concepto *") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = tripTextFieldColors()
            )

            OutlinedTextField(
                value = importe,
                onValueChange = { importe = it },
                label = { Text("Importe (€) *") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = tripTextFieldColors()
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

            if (error.isNotEmpty()) {
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    when {
                        concepto.isBlank() -> error = "El concepto es obligatorio"
                        importe.isBlank() -> error = "El importe es obligatorio"
                        importe.toDoubleOrNull() == null -> error = "El importe debe ser un número"
                        importe.toDouble() <= 0 -> error = "El importe debe ser mayor que 0"
                        else -> {
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