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

/**
 * Pantalla que muestra y gestiona los participantes de un viaje.
 * Permite añadir nuevos participantes por email y eliminar los existentes.
 *
 * El usuario autenticado no aparece en la lista — solo ve a los demás
 * participantes. El propietario (Admin) no puede ser eliminado.
 *
 * Límite de participantes: máximo 5 por viaje. Si se alcanza el límite,
 * el botón FAB cambia de color y se muestra un diálogo informativo.
 *
 * Los participantes se sincronizan en tiempo real desde Firestore y se
 * cachean en Room en cada actualización para acceso offline.
 *
 * @param idViaje Identificador del viaje cuyos participantes se gestionan
 * @param repository Repositorio para obtener y gestionar participantes en Firestore
 */
@Composable
fun ParticipantesScreen(
    idViaje: String,
    repository: FirestoreViajeRepository
) {
    val participantesViewModel: ParticipantesViewModel = viewModel(
        factory = ParticipantesViewModelFactory(repository)
    )
    val uiState by participantesViewModel.uiState.collectAsState()

    /** Lista de participantes en tiempo real desde Firestore */
    val participantes by repository.obtenerParticipantes(idViaje).collectAsState(initial = emptyList())

    /** UID del usuario autenticado para filtrarlo de la lista */
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    /** Lista filtrada sin el usuario autenticado — el usuario ya sabe que está en el viaje */
    val participantesFiltrados = participantes.filter {
        it["idUsuario"]?.toString() != currentUserId
    }

    val mostrarDialogoAnadir = remember { mutableStateOf(false) }
    val mostrarLimiteAlcanzado = remember { mutableStateOf(false) }
    val emailNuevo = remember { mutableStateOf("") }
    val error = remember { mutableStateOf("") }

    /** Participante seleccionado para eliminar — activa el diálogo de confirmación */
    val participanteAEliminar = remember { mutableStateOf<Map<String, Any>?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Sincronizar participantes en Room cada vez que cambia la lista de Firestore
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

                            // Cachear en Room usando el formato "{idViaje}_{idUsuario}" como id
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

    // Reaccionar a los cambios de estado del ViewModel para cerrar el diálogo o mostrar errores
    LaunchedEffect(uiState) {
        when (uiState) {
            is ParticipanteUiState.Success -> {
                // Cerrar el diálogo y limpiar el formulario al añadir con éxito
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

    // Diálogo informativo cuando se alcanza el límite de 5 participantes
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

    // Diálogo para añadir un nuevo participante por email
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
                    // Deshabilitar el botón mientras se procesa la petición
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

    // Diálogo de confirmación antes de eliminar un participante
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
        // Estado vacío cuando no hay participantes además del usuario actual
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
                // Contador de participantes — se muestra en rojo si se ha alcanzado el límite
                item {
                    Text(
                        text = "${participantes.size}/5 participantes",
                        fontSize = 12.sp,
                        color = if (participantes.size >= 5) MaterialTheme.colorScheme.error else TripTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Lista de participantes filtrados (sin el usuario actual)
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
                            // Avatar circular con icono de persona
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
                            // El Admin muestra badge y no puede ser eliminado
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
                                // Los participantes no admin pueden ser eliminados
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

        // FAB para añadir participante — gris si se ha alcanzado el límite
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