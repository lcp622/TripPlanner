package dam.pmdm.tripplanner.ui.gastos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dam.pmdm.tripplanner.data.local.TripPlannerDatabase
import dam.pmdm.tripplanner.data.local.entity.GastoEntity
import dam.pmdm.tripplanner.data.repository.GastoRepository
import dam.pmdm.tripplanner.ui.theme.*
import kotlinx.coroutines.tasks.await

@Composable
fun GastosScreen() {
    val context = LocalContext.current
    val db = TripPlannerDatabase.getInstance(context)
    val gastoRepository = GastoRepository(db.gastoDao())
    val viewModel: GastoViewModel = viewModel(factory = GastoViewModelFactory(gastoRepository))

    var todosGastos by remember { mutableStateOf<List<GastoEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: return@LaunchedEffect
        val firestore = FirebaseFirestore.getInstance()

        // Obtener IDs de viajes del usuario
        val viajesSnapshot = firestore.collection("viajes")
            .whereEqualTo("idPropietario", user.uid)
            .get().await()

        val idViajes = viajesSnapshot.documents.map { it.id }

        // Obtener gastos de cada viaje
        val gastos = mutableListOf<GastoEntity>()
        idViajes.forEach { idViaje ->
            val gastosSnapshot = firestore.collection("viajes")
                .document(idViaje)
                .collection("gastos")
                .get().await()
            gastos.addAll(
                gastosSnapshot.documents.mapNotNull {
                    it.toObject(GastoEntity::class.java)
                }
            )
        }
        todosGastos = gastos.sortedByDescending { it.fecha }
        isLoading = false
    }

    val totalGastado = todosGastos.sumOf { it.importe }
    val gastosPorCategoria = todosGastos.groupBy { it.categoria }
        .mapValues { (_, gastos) -> gastos.sumOf { it.importe } }

    val categoriaColores = mapOf(
        "ALOJAMIENTO" to Color(0xFF9C27B0),
        "TRANSPORTE" to Color(0xFF2196F3),
        "COMIDA" to Color(0xFF4CAF50),
        "OCIO" to Color(0xFFFF9800),
        "OTROS" to Color(0xFF9E9E9E)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = TripBlue
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                item {
                    Text(
                        text = "Mis Gastos",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Resumen de todos tus viajes",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Card total
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = TripBlue)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Total gastado",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                            Text(
                                text = "€${String.format("%.2f", totalGastado)}",
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${todosGastos.size} gastos en ${gastosPorCategoria.size} categorías",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Por categoría
                if (gastosPorCategoria.isNotEmpty()) {
                    item {
                        Text(
                            text = "POR CATEGORÍA",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TripTextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(gastosPorCategoria.entries.sortedByDescending { it.value }.toList()) { (categoria, importe) ->
                        val color = categoriaColores[categoria] ?: TripGray
                        val porcentaje = if (totalGastado > 0) (importe / totalGastado * 100).toInt() else 0

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = when (categoria) {
                                                "ALOJAMIENTO" -> "🏨"
                                                "TRANSPORTE" -> "✈️"
                                                "COMIDA" -> "🍽️"
                                                "OCIO" -> "🎭"
                                                else -> "💳"
                                            },
                                            fontSize = 20.sp
                                        )
                                        Text(
                                            text = categoria,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "€${String.format("%.2f", importe)}",
                                            fontWeight = FontWeight.Bold,
                                            color = color
                                        )
                                        Text(
                                            text = "$porcentaje%",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { porcentaje / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = color,
                                    trackColor = color.copy(alpha = 0.1f)
                                )
                            }
                        }
                    }
                }

                // Gastos recientes
                if (todosGastos.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "GASTOS RECIENTES",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TripTextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(todosGastos.take(10)) { gasto ->
                        GastoCard(
                            gasto = gasto,
                            onEliminar = {},
                            onEditar = {}
                        )
                    }
                }

                if (todosGastos.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "💰", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No tienes gastos aún",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Añade gastos en tus viajes",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}