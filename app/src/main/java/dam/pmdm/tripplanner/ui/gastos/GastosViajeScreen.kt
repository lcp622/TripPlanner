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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import dam.pmdm.tripplanner.data.local.entity.GastoEntity
import dam.pmdm.tripplanner.data.repository.GastoRepository
import dam.pmdm.tripplanner.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pantalla que muestra los gastos de un viaje concreto.
 * Incluye un resumen del total gastado versus presupuesto, buscador,
 * filtros por categoría y la lista de gastos con sus repartos.
 *
 * @param idViaje Identificador del viaje cuyos gastos se muestran
 * @param presupuesto Presupuesto total del viaje para calcular el restante
 * @param viewModel ViewModel que gestiona la lógica de gastos
 * @param repository Repositorio para obtener los repartos en tiempo real
 * @param onNuevoGasto Callback que navega a la pantalla de crear gasto
 * @param onEditarGasto Callback que navega a la pantalla de editar gasto con el id del gasto
 */
@Composable
fun GastosViajeScreen(
    idViaje: String,
    presupuesto: Double,
    viewModel: GastoViewModel,
    repository: GastoRepository,
    onNuevoGasto: () -> Unit,
    onEditarGasto: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    /** Texto de búsqueda para filtrar gastos por concepto o pagador */
    var textoBusqueda by remember { mutableStateOf("") }

    /** Categoría seleccionada para filtrar — "TODAS" muestra todos los gastos */
    var filtroCategoria by remember { mutableStateOf("TODAS") }

    // Cargar los gastos del viaje al entrar en la pantalla
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
                // Aplicar filtros de búsqueda y categoría sobre la lista de gastos
                val gastosFiltrados = state.gastos.filter { gasto ->
                    (textoBusqueda.isBlank() ||
                            gasto.concepto.contains(textoBusqueda, ignoreCase = true) ||
                            gasto.nombrePagador.contains(textoBusqueda, ignoreCase = true)) &&
                            (filtroCategoria == "TODAS" || gasto.categoria == filtroCategoria)
                }

                // Estado vacío cuando no hay gastos en el viaje
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
                        // Tarjeta resumen con total gastado y presupuesto restante
                        item {
                            val totalGastado = viewModel.totalGastos(state.gastos)
                            val restante = presupuesto - totalGastado

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = TripBlue)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "Total gastado", color = Color.White, fontSize = 14.sp)
                                        Text(
                                            text = "€${String.format(LocalLocale.current.platformLocale, "%.2f", totalGastado)}",
                                            color = Color.White,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Presupuesto restante",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 13.sp
                                        )
                                        // Mostrar en rojo si se ha superado el presupuesto
                                        Text(
                                            text = "€${String.format(LocalLocale.current.platformLocale, "%.2f", restante)}",
                                            color = if (restante < 0) Color(0xFFFF5252) else Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Campo de búsqueda por concepto o pagador
                        item {
                            OutlinedTextField(
                                value = textoBusqueda,
                                onValueChange = { textoBusqueda = it },
                                label = { Text("Buscar gastos...") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = TripGray)
                                },
                                colors = tripTextFieldColors()
                            )
                        }

                        // Filtros por categoría con chips seleccionables
                        item {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("TODAS", "ALOJAMIENTO", "TRANSPORTE", "COMIDA", "OCIO", "OTROS").forEach { cat ->
                                    val emoji = when (cat) {
                                        "ALOJAMIENTO" -> "🏨"
                                        "TRANSPORTE" -> "✈️"
                                        "COMIDA" -> "🍽️"
                                        "OCIO" -> "🎭"
                                        "OTROS" -> "💳"
                                        else -> "🔍"
                                    }
                                    FilterChip(
                                        selected = filtroCategoria == cat,
                                        onClick = { filtroCategoria = cat },
                                        label = {
                                            Text(
                                                if (cat == "TODAS") "Todas" else emoji,
                                                fontSize = 12.sp
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = TripBlue,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }

                        // Estado vacío cuando el filtro no encuentra resultados
                        if (gastosFiltrados.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = "🔍", fontSize = 36.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "No se encontraron gastos",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(gastosFiltrados) { gasto ->
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

/**
 * Tarjeta que muestra los datos de un gasto con sus acciones y reparto.
 * Permite editar y eliminar el gasto, y expandir el reparto entre participantes.
 * El usuario actual puede marcar su parte del reparto como saldada.
 *
 * @param gasto Entidad del gasto a mostrar
 * @param onEliminar Callback que elimina el gasto
 * @param onEditar Callback que navega a la pantalla de edición
 * @param repository Repositorio para escuchar los repartos en tiempo real
 * @param viewModel ViewModel para marcar repartos como saldados
 */
@Composable
fun GastoCard(
    gasto: GastoEntity,
    onEliminar: () -> Unit,
    onEditar: () -> Unit,
    repository: GastoRepository,
    viewModel: GastoViewModel
) {
    /** Controla la visibilidad del diálogo de confirmación de eliminación */
    val mostrarDialogo = remember { mutableStateOf(false) }

    /** Controla si el reparto está expandido o colapsado */
    var expandido by remember { mutableStateOf(false) }

    /** Repartos del gasto obtenidos en tiempo real desde Firestore */
    val repartos by repository.obtenerRepartos(gasto.idViaje, gasto.idGasto)
        .collectAsState(initial = emptyList())

    /** UID del usuario autenticado para identificar su reparto */
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Diálogo de confirmación antes de eliminar el gasto
    if (mostrarDialogo.value) {
        AlertDialog(
            onDismissRequest = { mostrarDialogo.value = false },
            title = { Text("Eliminar gasto", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás segura de que quieres eliminar \"${gasto.concepto}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        onEliminar()
                        mostrarDialogo.value = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogo.value = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    val dateFormat = SimpleDateFormat("dd MMM yyyy", LocalLocale.current.platformLocale)

    /** Color asignado a la categoría del gasto */
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
                // Icono de la categoría con fondo semitransparente
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
                    if (gasto.nombrePagador.isNotBlank()) {
                        Text(
                            text = "💳 Pagado por ${gasto.nombrePagador}",
                            fontSize = 12.sp,
                            color = TripBlue.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
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
                        text = "€${String.format(LocalLocale.current.platformLocale, "%.2f", gasto.importe)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TripBlue
                    )
                    IconButton(onClick = onEditar, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = TripBlue, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { mostrarDialogo.value = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Sección de reparto — solo visible si hay repartos
            if (repartos.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                // Botón para expandir/colapsar el reparto
                TextButton(onClick = { expandido = !expandido }, modifier = Modifier.fillMaxWidth()) {
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

                // Lista de repartos por participante
                if (expandido) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repartos.forEach { reparto ->
                            val idUsuario = reparto["idUsuario"]?.toString() ?: ""
                            val nombre = reparto["nombreUsuario"]?.toString() ?: "Usuario"
                            val importe = (reparto["importeAsignado"] as? Double) ?: 0.0
                            val saldado = reparto["saldado"] as? Boolean ?: false

                            /** Indica si este reparto pertenece al usuario autenticado */
                            val esMiReparto = idUsuario == currentUserId

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Icono de estado: check si saldado, persona si pendiente
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (saldado) Color(0xFF4CAF50).copy(alpha = 0.15f) else TripBlue.copy(alpha = 0.15f)),
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
                                    Text(text = nombre, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        text = if (saldado) "Saldado ✓" else "Pendiente",
                                        fontSize = 11.sp,
                                        color = if (saldado) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                                    )
                                }
                                Text(
                                    text = "€${String.format(LocalLocale.current.platformLocale, "%.2f", importe)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (saldado) MaterialTheme.colorScheme.onSurfaceVariant else TripBlue
                                )
                                // Mostrar botón de saldar solo al usuario con reparto pendiente
                                if (esMiReparto && !saldado) {
                                    Button(
                                        onClick = { viewModel.marcarComoSaldado(gasto.idViaje, gasto.idGasto, currentUserId) },
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