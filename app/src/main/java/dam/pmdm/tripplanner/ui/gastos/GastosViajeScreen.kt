package dam.pmdm.tripplanner.ui.gastos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import dam.pmdm.tripplanner.data.local.entity.GastoEntity
import dam.pmdm.tripplanner.data.repository.GastoRepository
import dam.pmdm.tripplanner.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GastosViajeScreen(
    idViaje: String,
    viewModel: GastoViewModel,
    repository: GastoRepository,
    onNuevoGasto: () -> Unit,
    onEditarGasto: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(idViaje) {
        viewModel.cargarGastos(idViaje)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val state = uiState) {
            is GastoUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = TripBlue
                )
            }
            is GastoUiState.Error -> {
                Text(
                    text = state.mensaje,
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error
                )
            }
            is GastoUiState.Success -> {
                if (state.gastos.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "💰", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No hay gastos aún",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pulsa + para añadir un gasto",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = TripBlue)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Total gastado",
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "€${String.format("%.2f", viewModel.totalGastos(state.gastos))}",
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        items(state.gastos) { gasto ->
                            GastoCard(
                                gasto = gasto,
                                onEliminar = { viewModel.eliminarGasto(gasto) },
                                onEditar = { onEditarGasto(gasto.idGasto) },
                                repository = repository,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onNuevoGasto,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = TripBlue,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nuevo gasto")
        }
    }
}

@Composable
fun GastoCard(
    gasto: GastoEntity,
    onEliminar: () -> Unit,
    onEditar: () -> Unit,
    repository: GastoRepository,
    viewModel: GastoViewModel
) {
    var mostrarDialogo by remember { mutableStateOf(false) }
    var expandido by remember { mutableStateOf(false) }
    val repartos by repository.obtenerRepartos(gasto.idViaje, gasto.idGasto)
        .collectAsState(initial = emptyList())
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    if (mostrarDialogo) {
        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },
            title = { Text("Eliminar gasto", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás segura de que quieres eliminar \"${gasto.concepto}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        onEliminar()
                        mostrarDialogo = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogo = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val categoriaColor = when (gasto.categoria) {
        "ALOJAMIENTO" -> Color(0xFF9C27B0)
        "TRANSPORTE" -> Color(0xFF2196F3)
        "COMIDA" -> Color(0xFF4CAF50)
        "OCIO" -> Color(0xFFFF9800)
        else -> Color(0xFF9E9E9E)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(categoriaColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (gasto.categoria) {
                            "ALOJAMIENTO" -> "🏨"
                            "TRANSPORTE" -> "✈️"
                            "COMIDA" -> "🍽️"
                            "OCIO" -> "🎭"
                            else -> "💳"
                        },
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = gasto.concepto,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = dateFormat.format(Date(gasto.fecha)),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .background(categoriaColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = gasto.categoria,
                            fontSize = 11.sp,
                            color = categoriaColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "€${String.format("%.2f", gasto.importe)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TripBlue
                    )
                    IconButton(
                        onClick = onEditar,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = TripBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { mostrarDialogo = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Botón expandir reparto
            if (repartos.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                TextButton(
                    onClick = { expandido = !expandido },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (expandido) "Ocultar reparto" else "Ver reparto (${repartos.size})",
                        fontSize = 12.sp,
                        color = TripBlue
                    )
                    Icon(
                        if (expandido) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TripBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (expandido) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repartos.forEach { reparto ->
                            val idUsuario = reparto["idUsuario"]?.toString() ?: ""
                            val nombre = reparto["nombreUsuario"]?.toString() ?: "Usuario"
                            val importe = (reparto["importeAsignado"] as? Double) ?: 0.0
                            val saldado = reparto["saldado"] as? Boolean ?: false
                            val esMiReparto = idUsuario == currentUserId

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (saldado) Color(0xFF4CAF50).copy(alpha = 0.15f)
                                            else TripBlue.copy(alpha = 0.15f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (saldado) Icons.Default.Check else Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (saldado) Color(0xFF4CAF50) else TripBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = nombre,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (saldado) "Saldado ✓" else "Pendiente",
                                        fontSize = 11.sp,
                                        color = if (saldado) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                                    )
                                }
                                Text(
                                    text = "€${String.format("%.2f", importe)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (saldado) MaterialTheme.colorScheme.onSurfaceVariant else TripBlue
                                )
                                if (esMiReparto && !saldado) {
                                    Button(
                                        onClick = {
                                            viewModel.marcarComoSaldado(gasto.idViaje, gasto.idGasto, currentUserId)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Saldar", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}