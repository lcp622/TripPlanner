package dam.pmdm.tripplanner.ui.gastos

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dam.pmdm.tripplanner.data.local.entity.GastoEntity
import dam.pmdm.tripplanner.ui.theme.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pantalla que muestra un resumen global de todos los gastos del usuario.
 * Agrega los gastos de todos los viajes propios y compartidos del usuario,
 * mostrando el total gastado, un gráfico circular por categoría y
 * los gastos más recientes.
 *
 * Se carga directamente desde Firestore sin pasar por un ViewModel
 * para simplificar la implementación de esta vista de solo lectura.
 */
@Composable
fun GastosScreen() {
    /** Lista completa de gastos de todos los viajes del usuario */
    var todosGastos by remember { mutableStateOf<List<GastoEntity>>(emptyList()) }

    /** Mapa de idViaje a nombre del viaje para mostrar en cada gasto */
    var nombresViajes by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    /** Indica si los datos están cargándose desde Firestore */
    var isLoading by remember { mutableStateOf(true) }

    // Cargar todos los gastos del usuario al entrar en la pantalla
    LaunchedEffect(Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: return@LaunchedEffect
        val firestore = FirebaseFirestore.getInstance()

        // Obtener viajes propios y compartidos
        val viajesPropios = firestore.collection("viajes")
            .whereEqualTo("idPropietario", user.uid)
            .get().await()

        val viajesParticipante = firestore.collection("viajes")
            .whereArrayContains("participantesIds", user.uid)
            .get().await()

        val todosViajes = (viajesPropios.documents + viajesParticipante.documents)
            .distinctBy { it.id }

        // Construir mapa de nombres de viajes para mostrar en cada gasto
        val nombres = mutableMapOf<String, String>()
        todosViajes.forEach { viajeDoc ->
            val nombre = viajeDoc.getString("nombre") ?: "Viaje"
            nombres[viajeDoc.id] = nombre
        }
        nombresViajes = nombres

        // Cargar gastos de cada viaje y ordenar por fecha descendente
        val gastos = mutableListOf<GastoEntity>()
        todosViajes.forEach { viajeDoc ->
            val gastosSnapshot = firestore.collection("viajes")
                .document(viajeDoc.id)
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

    /** Suma total de todos los gastos */
    val totalGastado = todosGastos.sumOf { it.importe }

    /** Gastos agrupados por categoría con su importe total */
    val gastosPorCategoria = todosGastos.groupBy { it.categoria }
        .mapValues { (_, gastos) -> gastos.sumOf { it.importe } }

    /** Colores asignados a cada categoría para el gráfico y tarjetas */
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
                // Cabecera de la pantalla
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

                // Tarjeta resumen con el total gastado
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
                                text = "€${String.format(LocalLocale.current.platformLocale, "%.2f", totalGastado)}",
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

                // Gráfico circular solo si hay datos
                if (gastosPorCategoria.isNotEmpty() && totalGastado > 0) {
                    item {
                        GraficoPorCategoria(
                            gastosPorCategoria = gastosPorCategoria,
                            totalGastado = totalGastado,
                            categoriaColores = categoriaColores
                        )
                    }
                }

                // Sección de gastos por categoría con barra de progreso
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

                    // Ordenar categorías por importe descendente
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
                                    Text(
                                        text = "€${String.format(LocalLocale.current.platformLocale, "%.2f", importe)}",
                                        fontWeight = FontWeight.Bold,
                                        color = color
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                // Barra de progreso proporcional al porcentaje del total
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

                // Sección de gastos recientes — máximo 10 para no saturar la pantalla
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
                        GastoResumenCard(
                            gasto = gasto,
                            nombreViaje = nombresViajes[gasto.idViaje] ?: "Viaje"
                        )
                    }
                }

                // Estado vacío cuando no hay gastos
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

/**
 * Componente que muestra un gráfico circular (donut) con la distribución
 * de gastos por categoría y una leyenda con los importes.
 *
 * Se elige un gráfico circular porque permite visualizar de forma intuitiva
 * la proporción de cada categoría sobre el total gastado.
 *
 * @param gastosPorCategoria Mapa de categoría a importe total
 * @param totalGastado Suma total de todos los gastos para calcular porcentajes
 * @param categoriaColores Mapa de categoría a color para el gráfico
 */
@Composable
fun GraficoPorCategoria(
    gastosPorCategoria: Map<String, Double>,
    totalGastado: Double,
    categoriaColores: Map<String, Color>
) {
    /** Lista de pares categoría-ángulo calculados proporcionalmente al total */
    val angulos = gastosPorCategoria.entries.map { (categoria, importe) ->
        categoria to (importe / totalGastado * 360f).toFloat()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Distribución de gastos",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Dibujar gráfico circular con un círculo blanco central (efecto donut)
            Canvas(modifier = Modifier.size(180.dp)) {
                var startAngle = -90f
                angulos.forEach { (categoria, angulo) ->
                    val color = categoriaColores[categoria] ?: Color.Gray
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = angulo,
                        useCenter = true,
                        size = Size(size.width, size.height)
                    )
                    startAngle += angulo
                }
                // Círculo blanco central para crear efecto donut
                drawCircle(
                    color = Color.White,
                    radius = size.minDimension / 3.5f
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Leyenda con color, icono, categoría e importe
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                gastosPorCategoria.entries.sortedByDescending { it.value }.forEach { (categoria, importe) ->
                    val color = categoriaColores[categoria] ?: Color.Gray
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(color, CircleShape)
                        )
                        Text(
                            text = when (categoria) {
                                "ALOJAMIENTO" -> "🏨"
                                "TRANSPORTE" -> "✈️"
                                "COMIDA" -> "🍽️"
                                "OCIO" -> "🎭"
                                else -> "💳"
                            },
                            fontSize = 14.sp
                        )
                        Text(
                            text = categoria,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "€${String.format(LocalLocale.current.platformLocale, "%.2f", importe)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta que muestra el resumen de un gasto individual.
 * Muestra el icono de la categoría, concepto, fecha, viaje e importe.
 *
 * @param gasto Entidad del gasto a mostrar
 * @param nombreViaje Nombre del viaje al que pertenece el gasto
 */
@Composable
fun GastoResumenCard(gasto: GastoEntity, nombreViaje: String = "") {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", LocalLocale.current.platformLocale)

    /** Color asignado a la categoría del gasto */
    val categoriaColor = when (gasto.categoria) {
        "ALOJAMIENTO" -> Color(0xFF9C27B0)
        "TRANSPORTE" -> Color(0xFF2196F3)
        "COMIDA" -> Color(0xFF4CAF50)
        "OCIO" -> Color(0xFFFF9800)
        else -> Color(0xFF9E9E9E)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono de la categoría con fondo semitransparente
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(categoriaColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (gasto.categoria) {
                        "ALOJAMIENTO" -> "🏨"
                        "TRANSPORTE" -> "✈️"
                        "COMIDA" -> "🍽️"
                        "OCIO" -> "🎭"
                        else -> "💳"
                    },
                    fontSize = 20.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = gasto.concepto,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = dateFormat.format(Date(gasto.fecha)),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Mostrar el nombre del viaje si está disponible
                if (nombreViaje.isNotEmpty()) {
                    Text(
                        text = "✈️ $nombreViaje",
                        fontSize = 11.sp,
                        color = TripBlue.copy(alpha = 0.8f)
                    )
                }
            }
            Text(
                text = "€${String.format(LocalLocale.current.platformLocale, "%.2f", gasto.importe)}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TripBlue
            )
        }
    }
}