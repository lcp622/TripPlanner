package dam.pmdm.tripplanner.ui.itinerario

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dam.pmdm.tripplanner.data.local.entity.ActividadEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ItinerarioScreen(
    viewModel: ActividadViewModel,
    onNuevaActividad: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is ActividadUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is ActividadUiState.Error -> {
                Text(
                    text = state.mensaje,
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error
                )
            }
            is ActividadUiState.Success -> {
                if (state.actividades.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No hay actividades aún",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pulsa + para añadir una actividad",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.actividades) { actividad ->
                            ActividadCard(
                                actividad = actividad,
                                onEliminar = { viewModel.eliminarActividad(actividad) }
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onNuevaActividad,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nueva actividad")
        }
    }
}

@Composable
fun ActividadCard(
    actividad: ActividadEntity,
    onEliminar: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = actividad.titulo,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (actividad.horaInicio != null) {
                    Text(
                        text = "${actividad.horaInicio} - ${actividad.horaFin ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (actividad.lugar != null) {
                    Text(
                        text = actividad.lugar,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = dateFormat.format(Date(actividad.fecha)),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            IconButton(onClick = onEliminar) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}