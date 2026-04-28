package dam.pmdm.tripplanner.ui.viajes

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dam.pmdm.tripplanner.data.local.entity.ViajeEntity
import dam.pmdm.tripplanner.ui.itinerario.ActividadViewModel
import dam.pmdm.tripplanner.ui.itinerario.ItinerarioScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleViajeScreen(
    viaje: ViajeEntity,
    actividadViewModel: ActividadViewModel,
    onVolver: () -> Unit,
    onNuevaActividad: () -> Unit
) {
    var tabSeleccionada by remember { mutableIntStateOf(0) }
    val tabs = listOf("Itinerario", "Gastos", "Rutas")

    LaunchedEffect(viaje.idViaje) {
        actividadViewModel.cargarActividades(viaje.idViaje)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viaje.nombre) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Info del viaje
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = viaje.paisDestino,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Presupuesto: ${viaje.presupuestoTotal}€",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = viaje.estado,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Pestañas
            TabRow(selectedTabIndex = tabSeleccionada) {
                tabs.forEachIndexed { index, titulo ->
                    Tab(
                        selected = tabSeleccionada == index,
                        onClick = { tabSeleccionada = index },
                        text = { Text(titulo) }
                    )
                }
            }

            // Contenido de cada pestaña
            when (tabSeleccionada) {
                0 -> ItinerarioScreen(
                    viewModel = actividadViewModel,
                    onNuevaActividad = onNuevaActividad
                )
                1 -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text("Módulo de gastos — próximamente")
                }
                2 -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text("Módulo de rutas — próximamente")
                }
            }
        }
    }
}