package dam.pmdm.tripplanner.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dam.pmdm.tripplanner.ui.theme.*

/**
 * Pantalla de registro de nuevos usuarios en TripPlanner.
 * Muestra un formulario con nombre, email, contraseña y confirmación
 * sobre un fondo degradado idéntico al de [LoginScreen].
 *
 * Realiza dos niveles de validación:
 * - [errorLocal]: validaciones locales antes de llamar al ViewModel
 *   (campos vacíos, contraseñas no coinciden, longitud mínima)
 * - [AuthUiState.Error]: errores devueltos por Firebase Auth
 *   (email ya registrado, contraseña débil, etc.)
 *
 * @param onRegistroExitoso Callback que se ejecuta al registrarse correctamente
 * @param onVolverALogin Callback que navega de vuelta a la pantalla de login
 * @param viewModel ViewModel que gestiona la lógica de autenticación
 */
@Composable
fun RegisterScreen(
    onRegistroExitoso: () -> Unit,
    onVolverALogin: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmarPassword by remember { mutableStateOf("") }

    /** Error de validación local mostrado antes de llamar al ViewModel */
    var errorLocal by remember { mutableStateOf("") }

    // Navegar automáticamente cuando el registro es exitoso
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onRegistroExitoso()
            viewModel.resetState()
        }
    }

    // Fondo con degradado vertical en colores de la app
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(TripTeal, TripBlue, TripBlueDark)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icono de la app
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "✈", fontSize = 36.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "TripPlanner",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Crea tu cuenta y empieza a explorar",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Tarjeta blanca con el formulario de registro
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Crear cuenta",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TripTextPrimary
                    )
                    Text(
                        text = "Rellena tus datos para registrarte",
                        fontSize = 13.sp,
                        color = TripTextSecondary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Campo de nombre
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre *", color = TripTextPrimary) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TripTextPrimary,
                            unfocusedTextColor = TripTextPrimary,
                            focusedBorderColor = TripBlue,
                            unfocusedBorderColor = TripGray,
                            focusedLabelColor = TripBlue,
                            unfocusedLabelColor = TripTextSecondary,
                            cursorColor = TripBlue
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Campo de email
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email *", color = TripTextPrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TripTextPrimary,
                            unfocusedTextColor = TripTextPrimary,
                            focusedBorderColor = TripBlue,
                            unfocusedBorderColor = TripGray,
                            focusedLabelColor = TripBlue,
                            unfocusedLabelColor = TripTextSecondary,
                            cursorColor = TripBlue
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Campo de contraseña con caracteres ocultos
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña *", color = TripTextPrimary) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TripTextPrimary,
                            unfocusedTextColor = TripTextPrimary,
                            focusedBorderColor = TripBlue,
                            unfocusedBorderColor = TripGray,
                            focusedLabelColor = TripBlue,
                            unfocusedLabelColor = TripTextSecondary,
                            cursorColor = TripBlue
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Campo de confirmación de contraseña
                    OutlinedTextField(
                        value = confirmarPassword,
                        onValueChange = { confirmarPassword = it },
                        label = { Text("Confirmar contraseña *", color = TripTextPrimary) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TripTextPrimary,
                            unfocusedTextColor = TripTextPrimary,
                            focusedBorderColor = TripBlue,
                            unfocusedBorderColor = TripGray,
                            focusedLabelColor = TripBlue,
                            unfocusedLabelColor = TripTextSecondary,
                            cursorColor = TripBlue
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Mostrar error de validación local
                    if (errorLocal.isNotEmpty()) {
                        Text(
                            text = errorLocal,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                    }

                    // Mostrar error devuelto por Firebase Auth
                    if (uiState is AuthUiState.Error) {
                        Text(
                            text = (uiState as AuthUiState.Error).mensaje,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botón de registro con validación local previa
                    Button(
                        onClick = {
                            // Validar campos antes de llamar al ViewModel
                            when {
                                nombre.isBlank() -> errorLocal = "El nombre es obligatorio"
                                email.isBlank() -> errorLocal = "El email es obligatorio"
                                password != confirmarPassword -> errorLocal = "Las contraseñas no coinciden"
                                password.length < 6 -> errorLocal = "La contraseña debe tener al menos 6 caracteres"
                                else -> {
                                    errorLocal = ""
                                    viewModel.registrar(email, password, nombre)
                                }
                            }
                        },
                        enabled = uiState !is AuthUiState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TripBlue)
                    ) {
                        // Mostrar spinner durante la carga o texto normal
                        if (uiState is AuthUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White
                            )
                        } else {
                            Text(
                                "Crear cuenta",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Enlace para volver al login
                    TextButton(
                        onClick = onVolverALogin,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "¿Ya tienes cuenta? Inicia sesión",
                            color = TripBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}