package dam.pmdm.tripplanner.ui.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import dam.pmdm.tripplanner.ui.SettingsViewModel
import dam.pmdm.tripplanner.ui.theme.*

@Composable
fun PerfilScreen(
    settingsViewModel: SettingsViewModel,
    onCerrarSesion: () -> Unit,
    onEditarPerfil: () -> Unit
) {
    val isDarkMode by settingsViewModel.isDarkMode.collectAsState()
    val user = FirebaseAuth.getInstance().currentUser
    val mostrarDialogoCerrarSesion = remember { mutableStateOf(false) }
    val mostrarDialogoBorrarCuenta = remember { mutableStateOf(false) }
    val errorBorrar = remember { mutableStateOf("") }

    if (mostrarDialogoCerrarSesion.value) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoCerrarSesion.value = false },
            title = { Text("Cerrar sesión", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás segura de que quieres cerrar sesión?") },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarDialogoCerrarSesion.value = false
                        onCerrarSesion()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TripBlue)
                ) {
                    Text("Cerrar sesión")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoCerrarSesion.value = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (mostrarDialogoBorrarCuenta.value) {
        AlertDialog(
            onDismissRequest = {
                mostrarDialogoBorrarCuenta.value = false
                errorBorrar.value = ""
            },
            title = { Text("Eliminar cuenta", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("¿Estás segura de que quieres eliminar tu cuenta? Esta acción no se puede deshacer y perderás todos tus datos.")
                    if (errorBorrar.value.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorBorrar.value,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uid = user?.uid ?: return@Button
                        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

                        db.collection("usuarios").document(uid).delete()

                        db.collection("viajes")
                            .whereEqualTo("idPropietario", uid)
                            .get()
                            .addOnSuccessListener { snapshot ->
                                snapshot.documents.forEach { viajeDoc ->
                                    listOf("actividades", "gastos", "participantes", "puntos_interes").forEach { sub ->
                                        viajeDoc.reference.collection(sub).get()
                                            .addOnSuccessListener { subDocs ->
                                                subDocs.documents.forEach { it.reference.delete() }
                                            }
                                    }
                                    viajeDoc.reference.delete()
                                }
                            }

                        // Eliminar cuenta de Firebase Auth
                        user.delete()
                            .addOnSuccessListener {
                                mostrarDialogoBorrarCuenta.value = false
                                onCerrarSesion()
                            }
                            .addOnFailureListener { e ->
                                errorBorrar.value = if (e.message?.contains("recent") == true) {
                                    "Por seguridad, cierra sesión y vuelve a iniciarla antes de eliminar la cuenta."
                                } else {
                                    "Error al eliminar la cuenta: ${e.message}"
                                }
                            }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar cuenta")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarDialogoBorrarCuenta.value = false
                    errorBorrar.value = ""
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(TripBlue.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            if (user?.photoUrl != null) {
                AsyncImage(
                    model = user.photoUrl.toString(),
                    contentDescription = "Foto de perfil",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = TripBlue,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = user?.displayName ?: user?.email?.substringBefore("@") ?: "Usuario",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = user?.email ?: "",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onEditarPerfil,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TripBlue),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Editar perfil", fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "AJUSTES",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TripTextSecondary,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = null,
                        tint = TripBlue
                    )
                    Column {
                        Text(
                            text = "Modo oscuro",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isDarkMode) "Activado" else "Desactivado",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { settingsViewModel.toggleDarkMode(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TripWhite,
                        checkedTrackColor = TripBlue
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth(),
            onClick = { mostrarDialogoCerrarSesion.value = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Cerrar sesión",
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth(),
            onClick = { mostrarDialogoBorrarCuenta.value = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Eliminar cuenta",
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}