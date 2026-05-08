package dam.pmdm.tripplanner.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
 * Pantalla de inicio de sesión de TripPlanner.
 * Muestra un formulario con email y contraseña sobre un fondo degradado.
 *
 * Gestiona tres estados de UI a través de [AuthViewModel.uiState]:
 * - Loading: muestra un indicador circular en el botón
 * - Success: navega automáticamente a la pantalla principal
 * - Error: muestra el mensaje de error bajo el formulario
 *
 * @param onLoginExitoso Callback que se ejecuta al iniciar sesión correctamente
 * @param onIrARegistro Callback que navega a la pantalla de registro
 * @param viewModel ViewModel que gestiona la lógica de autenticación
 */
@Composable
fun LoginScreen(
    onLoginExitoso: () -> Unit,
    onIrARegistro: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Navegar automáticamente cuando el login es exitoso
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onLoginExitoso()
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
                .padding(24.dp),
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
                text = "Planifica, comparte y explora en compañía",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Tarjeta blanca con el formulario de login
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "¡Bienvenido!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TripTextPrimary
                    )
                    Text(
                        text = "Inicia sesión para comenzar tus aventuras",
                        fontSize = 13.sp,
                        color = TripTextSecondary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Campo de email
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email", color = TripTextPrimary) },
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
                        label = { Text("Contraseña", color = TripTextPrimary) },
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

                    // Mostrar mensaje de error si el login falla
                    if (uiState is AuthUiState.Error) {
                        Text(
                            text = (uiState as AuthUiState.Error).mensaje,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botón de login — deshabilitado durante la carga
                    Button(
                        onClick = { viewModel.login(email, password) },
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
                                "Iniciar sesión",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Enlace para navegar al registro
                    TextButton(
                        onClick = onIrARegistro,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "¿No tienes cuenta? Regístrate",
                            color = TripBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}