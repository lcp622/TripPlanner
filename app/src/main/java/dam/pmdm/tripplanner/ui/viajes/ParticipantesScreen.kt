package dam.pmdm.tripplanner.ui.viajes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import dam.pmdm.tripplanner.data.local.TripPlannerDatabase
import dam.pmdm.tripplanner.data.local.entity.ParticipanteEntity
import dam.pmdm.tripplanner.data.repository.FirestoreViajeRepository
import dam.pmdm.tripplanner.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ParticipantesScreen(
    idViaje: String,
    repository: FirestoreViajeRepository
) {
    val participantesViewModel: ParticipantesViewModel = viewModel(
        factory = ParticipantesViewModelFactory(repository)
    )
    val uiState by participantesViewModel.uiState.collectAsState()
    val participantes by repository.obtenerParticipantes(idViaje).collectAsState(initial = emptyList())
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val participantesFiltrados = participantes.filter {
        it["idUsuario"]?.toString() != currentUserId
    }

    val mostrarDialogoAnadir = remember { mutableStateOf(false) }
    val mostrarLimiteAlcanzado = remember { mutableStateOf(false) }
    val emailNuevo = remember { mutableStateOf("") }
    val error = remember { mutableStateOf("") }
    val participanteAEliminar = remember { mutableStateOf<Map<String, Any>?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(participantes) {
        if (participantes.isNotEmpty()) {
            try {
                val db = TripPlannerDatabase.getInstance(context)
                scope.launch {
                    participantes.forEach { participante ->
                        try {
                            val idUsuario = participante["idUsuario"]?.toString() ?: return@forEach
                            val esAdmin = participante["esAdmin"] as? Boolean ?: false
                            val fechaUnion = (participante["fechaUnion"] as? Long) ?: System.currentTimeMillis()
                            db.participanteDao().insertar(
                                ParticipanteEntity(
                                    idParticipante = "${idViaje}_${idUsuario}",
                                    idViaje = idViaje,
                                    idUsuario = idUsuario,
                                    fechaUnion = fechaUnion,
                                    esAdmin = esAdmin
                                )
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("ParticipantesScreen", "Error insertando participante: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ParticipantesScreen", "Error: ${e.message}")
            }
        }
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is ParticipanteUiState.Success -> {
                mostrarDialogoAnadir.value = false
                emailNuevo.value = ""
                error.value = ""
                participantesViewModel.resetState()
            }
            is ParticipanteUiState.Error -> {
                error.value = (uiState as ParticipanteUiState.Error).mensaje
            }
            else -> {}
        }
    }

    if (mostrarLimiteAlcanzado.value) {
        AlertDialog(
            onDismissRequest = { mostrarLimiteAlcanzado.value = false },
            title = { Text("Límite alcanzado", fontWeight = FontWeight.Bold) },
            text = { Text("Un viaje puede tener un máximo de 5 participantes.") },
            confirmButton = {
                TextButton(onClick = { mostrarLimiteAlcanzado.value = false }) {
                    Text("Entendido")
                }
            }
        )
    }

    if (mostrarDialogoAnadir.value) {
        AlertDialog(
            onDismissRequest = {
                mostrarDialogoAnadir.value = false
                emailNuevo.value = ""
                error.value = ""
                participantesViewModel.resetState()
            },
            title = { Text("Añadir participante", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Introduce el email del usuario que quieres añadir",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = emailNuevo.value,
                        onValueChange = { emailNuevo.value = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = tripTextFieldColors()
                    )
                    if (error.value.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error.value,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (emailNuevo.value.isBlank()) {
                            error.value = "El email es obligatorio"
                        } else {
                            error.value = ""
                            participantesViewModel.anadirParticipante(idViaje, emailNuevo.value)
                        }
                    },
                    enabled = uiState !is ParticipanteUiState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = TripBlue)
                ) {
                    if (uiState is ParticipanteUiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text("Añadir")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarDialogoAnadir.value = false
                    emailNuevo.value = ""
                    error.value = ""
                    participantesViewModel.resetState()
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    participanteAEliminar.value?.let { p ->
        AlertDialog(
            onDismissRequest = { participanteAEliminar.value = null },
            title = { Text("Eliminar participante", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás segura de que quieres eliminar a \"${p["nombre"]}\" del viaje?") },
            confirmButton = {
                Button(
                    onClick = {
                        val idUsuarioEliminar = p["idUsuario"]?.toString() ?: ""
                        participantesViewModel.eliminarParticipante(idViaje, idUsuarioEliminar)
                        participanteAEliminar.value = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { participanteAEliminar.value = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (participantesFiltrados.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "👥", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No hay participantes",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pulsa + para añadir participantes (máx. 5)",
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
                    Text(
                        text = "${participantes.size}/5 participantes",
                        fontSize = 12.sp,
                        color = if (participantes.size >= 5) MaterialTheme.colorScheme.error else TripTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                items(participantesFiltrados) { participante ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(TripBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = TripBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = participante["nombre"]?.toString() ?: "Usuario",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = participante["email"]?.toString() ?: "",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (participante["esAdmin"] == true) {
                                Box(
                                    modifier = Modifier
                                        .background(TripBlue.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Admin",
                                        fontSize = 11.sp,
                                        color = TripBlue,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = { participanteAEliminar.value = participante }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Eliminar participante",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                if (participantes.size >= 5) {
                    mostrarLimiteAlcanzado.value = true
                } else {
                    mostrarDialogoAnadir.value = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = if (participantes.size >= 5) TripGray else TripBlue
        ) {
            Icon(Icons.Default.Add, contentDescription = "Añadir participante")
        }
    }
}