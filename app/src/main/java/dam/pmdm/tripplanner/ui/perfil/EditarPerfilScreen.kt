package dam.pmdm.tripplanner.ui.perfil

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import dam.pmdm.tripplanner.ui.auth.AuthUiState
import dam.pmdm.tripplanner.ui.auth.AuthViewModel
import dam.pmdm.tripplanner.ui.theme.*
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Pantalla que permite al usuario modificar sus datos de perfil (nombre y URL de foto).
 *
 * Esta pantalla utiliza [FirebaseAuth] para recuperar los datos actuales del usuario y
 * se apoya en el [AuthViewModel] para realizar las actualizaciones de forma asíncrona.
 *
 * Características principales:
 * - **Vista previa**: Muestra en tiempo real cómo quedaría la foto de perfil al introducir una URL.
 * - **Validación**: Asegura que el nombre no esté vacío antes de permitir el guardado.
 * - **Reactividad**: Observa el estado [AuthUiState] para cerrar la pantalla automáticamente
 *   cuando la actualización es exitosa.
 *
 * @param authViewModel ViewModel encargado de la lógica de autenticación y gestión del perfil.
 * @param onVolver Callback para navegar hacia atrás (al pulsar el botón volver o tras un éxito).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarPerfilScreen(
    authViewModel: AuthViewModel,
    onVolver: () -> Unit
) {
    // Referencia al usuario actual de Firebase
    val user = FirebaseAuth.getInstance().currentUser
    // Estado del flujo de actualización desde el ViewModel
    val perfilState by authViewModel.perfilState.collectAsState()

    // Estados locales para los campos del formulario
    var nombre by remember { mutableStateOf(user?.displayName ?: "") }
    var fotoUrl by remember { mutableStateOf(user?.photoUrl?.toString() ?: "") }
    var error by remember { mutableStateOf("") }

    /**
     * Efecto lanzado cuando el estado del perfil cambia.
     * Si la actualización es exitosa, resetea el estado y regresa a la pantalla anterior.
     */
    LaunchedEffect(perfilState) {
        if (perfilState is AuthUiState.Success) {
            authViewModel.resetPerfilState()
            onVolver()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Editar perfil",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Sección de vista previa de la imagen de perfil
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(TripBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (fotoUrl.isNotBlank()) {
                    AsyncImage(
                        model = fotoUrl,
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

            Text(
                text = "Vista previa de tu foto",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Campo: Nombre de usuario
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre *") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = tripTextFieldColors()
            )

            // Campo: URL de la foto
            OutlinedTextField(
                value = fotoUrl,
                onValueChange = { fotoUrl = it },
                label = { Text("URL de foto de perfil") },
                placeholder = { Text("https://ejemplo.com/foto.jpg") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = tripTextFieldColors()
            )

            // Visualización de errores de validación local
            if (error.isNotEmpty()) {
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }

            // Visualización de errores provenientes del servidor (ViewModel)
            if (perfilState is AuthUiState.Error) {
                Text(
                    text = (perfilState as AuthUiState.Error).mensaje,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Botón de acción para persistir cambios
            Button(
                onClick = {
                    if (nombre.isBlank()) {
                        error = "El nombre es obligatorio"
                    } else {
                        error = ""
                        authViewModel.actualizarPerfil(
                            nombre = nombre,
                            fotoUrl = fotoUrl.ifBlank { null }
                        )
                    }
                },
                enabled = perfilState !is AuthUiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = TripBlue)
            ) {
                if (perfilState is AuthUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("Guardar cambios", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}