package dam.pmdm.tripplanner.ui.viajes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dam.pmdm.tripplanner.data.local.entity.ViajeEntity
import dam.pmdm.tripplanner.data.repository.FirestoreViajeRepository
import dam.pmdm.tripplanner.data.repository.GastoRepository
import dam.pmdm.tripplanner.mostrarNotificacionColaborativa
import dam.pmdm.tripplanner.ui.gastos.GastoViewModel
import dam.pmdm.tripplanner.ui.gastos.GastosViajeScreen
import dam.pmdm.tripplanner.ui.itinerario.ActividadViewModel
import dam.pmdm.tripplanner.ui.itinerario.ItinerarioScreen
import dam.pmdm.tripplanner.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pantalla de detalle de un viaje con navegación por pestañas.
 * Muestra una cabecera con los datos principales del viaje y cuatro pestañas:
 * - **Itinerario**: lista de actividades del viaje
 * - **Gastos**: gestión de gastos y repartos
 * - **Viajeros**: gestión de participantes
 * - **Rutas**: mapa interactivo con puntos de interés
 *
 * Escucha en tiempo real los cambios en actividades y gastos de Firestore
 * para mostrar notificaciones colaborativas cuando otros participantes
 * añaden contenido. Se ignoran los cambios del propio usuario y la
 * carga inicial para evitar notificaciones falsas.
 *
 * @param viaje Entidad del viaje a mostrar
 * @param actividadViewModel ViewModel que gestiona las actividades
 * @param viajeViewModel ViewModel que gestiona las operaciones del viaje
 * @param gastoViewModel ViewModel que gestiona los gastos
 * @param gastoRepository Repositorio para obtener repartos en tiempo real
 * @param viajeRepository Repositorio para gestionar participantes
 * @param onVolver Callback que navega a la pantalla anterior
 * @param onNuevaActividad Callback que navega a crear actividad
 * @param onEditarActividad Callback que navega a editar actividad con su id
 * @param onNuevoGasto Callback que navega a crear gasto
 * @param onEditarGasto Callback que navega a editar gasto con su id
 * @param onViajeEliminado Callback que navega atrás tras eliminar el viaje
 * @param onEditarViaje Callback que navega a editar el viaje
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleViajeScreen(
    viaje: ViajeEntity,
    actividadViewModel: ActividadViewModel,
    viajeViewModel: ViajeViewModel,
    gastoViewModel: GastoViewModel,
    gastoRepository: GastoRepository,
    viajeRepository: FirestoreViajeRepository,
    onVolver: () -> Unit,
    onNuevaActividad: () -> Unit,
    onEditarActividad: (String) -> Unit,
    onNuevoGasto: () -> Unit,
    onEditarGasto: (String) -> Unit,
    onViajeEliminado: () -> Unit,
    onEditarViaje: () -> Unit
) {
    /** Índice de la pestaña seleccionada — se preserva en rotaciones con rememberSaveable */
    var tabSeleccionada by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Itinerario", "Gastos", "Viajeros", "Rutas")
    val dateFormat = SimpleDateFormat("dd MMM yyyy", LocalLocale.current.platformLocale)

    /** Controla la visibilidad del diálogo de confirmación de eliminación */
    val mostrarDialogoBorrar = remember { mutableStateOf(false) }
    val context = LocalContext.current

    /** UID del usuario autenticado para filtrar sus propias notificaciones */
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Cargar las actividades del viaje al entrar en la pantalla
    LaunchedEffect(viaje.idViaje) {
        actividadViewModel.cargarActividades(viaje.idViaje)
    }

    // Listeners de Firestore para notificaciones colaborativas en tiempo real
    DisposableEffect(viaje.idViaje) {
        // Flags para ignorar la carga inicial de datos y no generar notificaciones falsas
        var primeraVezActividades = true
        var primeraVezGastos = true

        // Listener de actividades: notifica cuando otro participante añade una actividad
        val listenerActividades = FirebaseFirestore.getInstance()
            .collection("viajes").document(viaje.idViaje)
            .collection("actividades")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                if (primeraVezActividades) {
                    primeraVezActividades = false
                    return@addSnapshotListener
                }
                snapshot.documentChanges.forEach { change ->
                    val idAutor = change.document.getString("idUsuario") ?: ""
                    // Solo notificar si la actividad la añadió otro participante
                    if (change.type.name == "ADDED" && idAutor != currentUserId) {
                        val titulo = change.document.getString("titulo") ?: "Nueva actividad"
                        mostrarNotificacionColaborativa(context, "Nueva actividad añadida", titulo)
                    }
                }
            }

        // Listener de gastos: notifica cuando otro participante añade un gasto
        val listenerGastos = FirebaseFirestore.getInstance()
            .collection("viajes").document(viaje.idViaje)
            .collection("gastos")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                if (primeraVezGastos) {
                    primeraVezGastos = false
                    return@addSnapshotListener
                }
                snapshot.documentChanges.forEach { change ->
                    val idAutor = change.document.getString("idPagador") ?: ""
                    // Solo notificar si el gasto lo añadió otro participante
                    if (change.type.name == "ADDED" && idAutor != currentUserId) {
                        val concepto = change.document.getString("concepto") ?: "Nuevo gasto"
                        mostrarNotificacionColaborativa(context, "Nuevo gasto añadido", concepto)
                    }
                }
            }

        // Eliminar listeners al salir de la pantalla para evitar fugas de memoria
        onDispose {
            listenerActividades.remove()
            listenerGastos.remove()
        }
    }

    /** Color del badge de estado según el estado del viaje */
    val estadoColor = when (viaje.estado) {
        "PLANIFICADO" -> ColorPlanificado
        "EN_CURSO" -> ColorEnCurso
        else -> ColorFinalizado
    }

    // Diálogo de confirmación antes de eliminar el viaje
    if (mostrarDialogoBorrar.value) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoBorrar.value = false },
            title = { Text("Eliminar viaje", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás segura de que quieres eliminar \"${viaje.nombre}\"? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        viajeViewModel.eliminarViaje(viaje.idViaje)
                        mostrarDialogoBorrar.value = false
                        onViajeEliminado()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoBorrar.value = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Cabecera con degradado y datos principales del viaje
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.verticalGradient(colors = listOf(TripTeal, TripBlue))
                    )
            ) {
                // Botón de volver en la esquina superior izquierda
                IconButton(
                    onClick = onVolver,
                    modifier = Modifier.padding(8.dp).align(Alignment.TopStart)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }

                // Botones de editar y eliminar en la esquina superior derecha
                Row(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                    IconButton(onClick = onEditarViaje) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar viaje", tint = Color.White)
                    }
                    IconButton(onClick = { mostrarDialogoBorrar.value = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar viaje", tint = Color.White)
                    }
                }

                // Nombre, destino y fechas en la parte inferior izquierda
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Text(
                        text = viaje.nombre,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = viaje.paisDestino,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${dateFormat.format(Date(viaje.fechaInicio))} → ${dateFormat.format(Date(viaje.fechaFin))}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                // Badge de estado en la esquina inferior derecha
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .background(estadoColor, RoundedCornerShape(8.dp))
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

            // Tarjeta resumen con presupuesto y duración del viaje
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "€${viaje.presupuestoTotal}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TripBlue
                        )
                        Text(
                            text = "Presupuesto",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.height(40.dp).width(1.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Calcular duración en días a partir de los milisegundos
                        val dias = ((viaje.fechaFin - viaje.fechaInicio) / (1000 * 60 * 60 * 24)).toInt()
                        Text(
                            text = "${dias}d",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TripBlue
                        )
                        Text(
                            text = "Duración",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Barra de pestañas con PrimaryTabRow para seguir el estilo Material3
            PrimaryTabRow(
                selectedTabIndex = tabSeleccionada,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = TripBlue
            ) {
                tabs.forEachIndexed { index, titulo ->
                    Tab(
                        selected = tabSeleccionada == index,
                        onClick = { tabSeleccionada = index },
                        text = {
                            Text(
                                titulo,
                                color = if (tabSeleccionada == index) TripBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (tabSeleccionada == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Contenido de la pestaña seleccionada
            when (tabSeleccionada) {
                0 -> ItinerarioScreen(
                    viewModel = actividadViewModel,
                    onNuevaActividad = onNuevaActividad,
                    onEditarActividad = onEditarActividad
                )
                1 -> GastosViajeScreen(
                    idViaje = viaje.idViaje,
                    presupuesto = viaje.presupuestoTotal,
                    viewModel = gastoViewModel,
                    repository = gastoRepository,
                    onNuevoGasto = onNuevoGasto,
                    onEditarGasto = onEditarGasto
                )
                2 -> ParticipantesScreen(
                    idViaje = viaje.idViaje,
                    repository = viajeRepository
                )
                3 -> RutasScreen(
                    idViaje = viaje.idViaje
                )
            }
        }
    }
}