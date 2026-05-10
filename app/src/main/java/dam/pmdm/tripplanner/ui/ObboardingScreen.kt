package dam.pmdm.tripplanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dam.pmdm.tripplanner.ui.theme.TripBlue
import dam.pmdm.tripplanner.ui.theme.TripTeal

data class OnboardingPagina(
    val emoji: String,
    val titulo: String,
    val descripcion: String
)

@Composable
fun OnboardingScreen(
    onTerminar: () -> Unit
) {
    val paginas = listOf(
        OnboardingPagina(
            emoji = "✈️",
            titulo = "Bienvenido a TripPlanner",
            descripcion = "Tu compañero ideal para planificar viajes increíbles con tus amigos y familia."
        ),
        OnboardingPagina(
            emoji = "🗺️",
            titulo = "Crea y comparte viajes",
            descripcion = "Crea viajes, invita a participantes y planificad juntos el itinerario en tiempo real."
        ),
        OnboardingPagina(
            emoji = "💰",
            titulo = "Gestiona los gastos",
            descripcion = "Añade gastos, repártelos entre los participantes y lleva el control del presupuesto."
        ),
        OnboardingPagina(
            emoji = "🌍",
            titulo = "Explora destinos",
            descripcion = "Descubre los mejores lugares de tu destino: museos, restaurantes, monumentos y más."
        )
    )

    var paginaActual by remember { mutableIntStateOf(0) }
    val pagina = paginas[paginaActual]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(TripTeal, TripBlue)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = pagina.emoji,
                    fontSize = 80.sp
                )

                Text(
                    text = pagina.titulo,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = pagina.descripcion,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Indicadores de página
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    paginas.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .size(if (index == paginaActual) 24.dp else 8.dp, 8.dp)
                                .background(
                                    color = if (index == paginaActual) Color.White else Color.White.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }

                // Botones de navegación
                if (paginaActual < paginas.size - 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onTerminar) {
                            Text(
                                text = "Omitir",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 16.sp
                            )
                        }

                        Button(
                            onClick = { paginaActual++ },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Siguiente",
                                color = TripBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onTerminar,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "¡Empezar!",
                            color = TripBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}