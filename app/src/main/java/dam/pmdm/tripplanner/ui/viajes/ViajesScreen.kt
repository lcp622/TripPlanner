package dam.pmdm.tripplanner.ui.viajes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dam.pmdm.tripplanner.data.local.entity.ViajeEntity
import dam.pmdm.tripplanner.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pantalla principal de viajes que muestra la lista de viajes del usuario.
 * Incluye buscador por nombre/destino y filtros por estado del viaje.
 *
 * Gestiona tres estados de UI a través de [ViajeViewModel.uiState]:
 * - [ViajeUiState.Loading]: muestra un indicador circular
 * - [ViajeUiState.Error]: muestra el mensaje de error
 * - [ViajeUiState.Success]: muestra la lista filtrada de viajes
 *
 * Cuando la lista está vacía muestra un estado vacío con instrucciones.
 * Cuando el filtro no encuentra resultados muestra un mensaje de búsqueda vacía.
 *
 * @param viewModel ViewModel que gestiona la lista de viajes
 * @param onNuevoViaje Callback que navega a la pantalla de crear viaje
 * @param onViajeClick Callback que navega al detalle del viaje con su id
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViajesScreen(
    viewModel: ViajeViewModel,
    onNuevoViaje: () -> Unit,
    onViajeClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    /** Texto de búsqueda para filtrar viajes por nombre o destino */
    var textoBusqueda by remember { mutableStateOf("") }

    /** Estado seleccionado para filtrar — "TODOS" muestra todos los viajes */
    var filtroEstado by remember { mutableStateOf("TODOS") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Mis Viajes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        // Subtítulo dinámico con el número de viajes cargados
                        Text(
                            text = when (val s = uiState) {
                                is ViajeUiState.Success -> "${s.viajes.size} viajes planificados"
                                else -> ""
                            },
                            fontSize = 12.sp,
                            color = TripTextSecondary
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = onNuevoViaje,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TripBlue),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nuevo", fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                // Estado de carga inicial
                is ViajeUiState.Loading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = TripBlue)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Cargando viajes...", color = TripTextSecondary)
                    }
                }
                // Estado de error al cargar desde Firestore
                is ViajeUiState.Error -> {
                    Text(
                        text = state.mensaje,
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is ViajeUiState.Success -> {
                    // Aplicar filtros de búsqueda y estado sobre la lista de viajes
                    val viajesFiltrados = state.viajes
                        .filter { viaje ->
                            (textoBusqueda.isBlank() ||
                                    viaje.nombre.contains(textoBusqueda, ignoreCase = true) ||
                                    viaje.paisDestino.contains(textoBusqueda, ignoreCase = true)) &&
                                    (filtroEstado == "TODOS" || viaje.estado == filtroEstado)
                        }

                    // Estado vacío cuando el usuario no tiene ningún viaje
                    if (state.viajes.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "✈️", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No tienes viajes aún",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Pulsa + para crear tu primer viaje",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TripTextSecondary
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Campo de búsqueda por nombre o destino
                            item {
                                OutlinedTextField(
                                    value = textoBusqueda,
                                    onValueChange = { textoBusqueda = it },
                                    label = { Text("Buscar viajes...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    leadingIcon = {
                                        Icon(Icons.Default.Search, contentDescription = null, tint = TripGray)
                                    },
                                    colors = tripTextFieldColors()
                                )
                            }

                            // Chips de filtro por estado del viaje
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("TODOS", "PLANIFICADO", "EN_CURSO", "FINALIZADO").forEach { estado ->
                                        val label = when (estado) {
                                            "TODOS" -> "Todos"
                                            "PLANIFICADO" -> "Planificado"
                                            "EN_CURSO" -> "En curso"
                                            "FINALIZADO" -> "Finalizado"
                                            else -> estado
                                        }
                                        FilterChip(
                                            selected = filtroEstado == estado,
                                            onClick = { filtroEstado = estado },
                                            label = { Text(label, fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = TripBlue,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }

                            // Estado vacío cuando el filtro no encuentra resultados
                            if (viajesFiltrados.isEmpty()) {
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
                                                text = "No se encontraron viajes",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Lista de viajes filtrados
                                items(viajesFiltrados) { viaje ->
                                    ViajeCard(
                                        viaje = viaje,
                                        onClick = { onViajeClick(viaje.idViaje) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta visual que muestra el resumen de un viaje.
 * Tiene una cabecera con degradado azul que muestra nombre, destino y propietario,
 * una etiqueta de estado en la esquina superior derecha, y una sección inferior
 * con la descripción, fecha de inicio y presupuesto.
 *
 * @param viaje Entidad del viaje a mostrar
 * @param onClick Callback que se ejecuta al pulsar la tarjeta
 */
@Composable
fun ViajeCard(
    viaje: ViajeEntity,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", LocalLocale.current.platformLocale)

    /** Color del badge de estado según el estado del viaje */
    val estadoColor = when (viaje.estado) {
        "PLANIFICADO" -> ColorPlanificado
        "EN_CURSO" -> ColorEnCurso
        else -> ColorFinalizado
    }

    /** Degradado de la cabecera de la tarjeta en colores de la app */
    val gradientColors = listOf(
        TripBlue.copy(alpha = 0.8f),
        TripTeal.copy(alpha = 0.6f)
    )

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        // Cabecera con degradado y datos principales del viaje
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(Brush.linearGradient(gradientColors))
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = viaje.nombre,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = viaje.paisDestino,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                // Mostrar propietario solo si tiene nombre (viajes compartidos)
                if (viaje.nombrePropietario.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = viaje.nombrePropietario,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Badge de estado en la esquina superior derecha
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(estadoColor)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = when (viaje.estado) {
                        "PLANIFICADO" -> "Planificado"
                        "EN_CURSO" -> "En curso"
                        "FINALIZADO" -> "Finalizado"
                        else -> viaje.estado
                    },
                    fontSize = 11.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Sección inferior con descripción, fecha y presupuesto
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            // Mostrar descripción solo si existe, truncada a 2 líneas
            viaje.descripcion?.let {
                Text(
                    text = it,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = TripGray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = dateFormat.format(Date(viaje.fechaInicio)),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "€${viaje.presupuestoTotal}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}