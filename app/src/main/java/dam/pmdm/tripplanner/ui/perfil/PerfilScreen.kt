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

/**
 * Pantalla de Perfil y Ajustes de la aplicación.
 *
 * Esta pantalla centraliza la gestión de la cuenta del usuario y las preferencias globales.
 * Permite realizar las siguientes acciones:
 * - Visualizar información del usuario (Foto, nombre y email).
 * - Navegar a la edición del perfil.
 * - Cambiar entre el modo claro y oscuro.
 * - Cerrar la sesión de Firebase Auth.
 * - Eliminar permanentemente la cuenta y todos los datos asociados en Firestore.
 *
 * @param settingsViewModel ViewModel que gestiona las preferencias del sistema (como el Modo Oscuro).
 * @param onCerrarSesion Callback que se ejecuta cuando el usuario sale de su cuenta o la elimina.
 * @param onEditarPerfil Callback para navegar a la pantalla de edición de perfil.
 */
@Composable
fun PerfilScreen(
    settingsViewModel: SettingsViewModel,
    onCerrarSesion: () -> Unit,
    onEditarPerfil: () -> Unit
) {
    // Estado del modo oscuro recuperado de forma reactiva
    val isDarkMode by settingsViewModel.isDarkMode.collectAsState()
    val user = FirebaseAuth.getInstance().currentUser

    // Estados para el control de flujo de los diálogos de confirmación
    val mostrarDialogoCerrarSesion = remember { mutableStateOf(false) }
    val mostrarDialogoBorrarCuenta = remember { mutableStateOf(false) }
    val errorBorrar = remember { mutableStateOf("") }

    /**
     * Diálogo de confirmación para el cierre de sesión.
     */
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

    /**
     * Diálogo de confirmación para eliminar la cuenta.
     *
     * Este proceso es crítico y realiza un borrado en cascada manual:
     * 1. Elimina el documento del usuario en la colección "usuarios".
     * 2. Localiza todos los viajes donde el usuario es propietario.
     * 3. Borra recursivamente todas las subcolecciones de cada viaje (actividades, gastos, etc.).
     * 4. Elimina la identidad del usuario en Firebase Auth.
     */
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

                        // 1. Borrar perfil en Firestore
                        db.collection("usuarios").document(uid).delete()

                        // 2. Lógica de borrado de viajes y subcolecciones (cascada manual)
                        db.collection("viajes")
                            .whereEqualTo("idPropietario", uid)
                            .get()
                            .addOnSuccessListener { snapshot ->
                                snapshot.documents.forEach { viajeDoc ->
                                    // Firestore requiere borrar documentos de subcolecciones uno a uno
                                    listOf("actividades", "gastos", "participantes", "puntos_interes").forEach { sub ->
                                        viajeDoc.reference.collection(sub).get()
                                            .addOnSuccessListener { subDocs ->
                                                subDocs.documents.forEach { it.reference.delete() }
                                            }
                                    }
                                    viajeDoc.reference.delete()
                                }
                            }

                        // 3. Eliminar usuario de Firebase Authentication
                        user.delete()
                            .addOnSuccessListener {
                                mostrarDialogoBorrarCuenta.value = false
                                onCerrarSesion()
                            }
                            .addOnFailureListener { e ->
                                // Error común: requiere re-autenticación si la sesión es antigua
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

        // Imagen de perfil con carga asíncrona (Coil)
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

        // Datos del usuario (Nombre y Email)
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

        // Botón de acceso a edición de perfil
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

        // Cabecera de la sección de ajustes
        Text(
            text = "AJUSTES",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TripTextSecondary,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Tarjeta de control de Tema (Modo Oscuro/Claro)
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

        // Tarjeta de acción: Cerrar Sesión
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

        // Tarjeta de acción: Eliminar Cuenta (Acción destructiva)
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