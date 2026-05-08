package dam.pmdm.tripplanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dam.pmdm.tripplanner.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Modelo de datos que representa un punto de interés obtenido de OpenStreetMap.
 *
 * @property nombre Nombre del lugar obtenido de las etiquetas OSM
 * @property tipo Categoría del lugar (por ejemplo "🏛️ Museos")
 * @property emoji Emoji representativo de la categoría para mostrar en la UI
 */
data class PuntoInteres(
    val nombre: String,
    val tipo: String,
    val emoji: String
)

/**
 * Pantalla de exploración de destinos turísticos.
 * Permite buscar una ciudad o país y obtener una lista de puntos de interés
 * organizados por categoría usando dos APIs externas gratuitas:
 *
 * - **Nominatim** (OpenStreetMap): convierte el nombre del destino en coordenadas
 * - **Overpass API**: busca puntos de interés por tipo en un radio alrededor de las coordenadas
 *
 * Se elige esta implementación sin API key para evitar dependencias de servicios de pago
 * y simplificar el despliegue de la app en entornos académicos.
 *
 * Las peticiones HTTP se ejecutan en [Dispatchers.IO] para no bloquear el hilo principal.
 */
@Composable
fun ExplorarScreen() {
    /** Texto introducido por el usuario en el campo de búsqueda */
    var busqueda by remember { mutableStateOf("") }

    /** Nombre del destino encontrado por Nominatim para mostrar en los resultados */
    var destino by remember { mutableStateOf("") }

    /** Mapa de categoría a lista de puntos de interés encontrados */
    var puntos by remember { mutableStateOf<Map<String, List<PuntoInteres>>>(emptyMap()) }

    /** Indica si hay una búsqueda en curso */
    var isLoading by remember { mutableStateOf(false) }

    /** Mensaje de error a mostrar si la búsqueda falla */
    var error by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    /**
     * Ejecuta la búsqueda de puntos de interés para el destino introducido.
     * Pasos realizados:
     * 1. Geocodifica el destino con Nominatim para obtener coordenadas
     * 2. Consulta Overpass API con una query que busca varios tipos de lugares
     * 3. Parsea los resultados y los agrupa por categoría (máx. 15 por categoría)
     */
    fun buscarDestino() {
        if (busqueda.isBlank()) return
        scope.launch {
            isLoading = true
            error = ""
            puntos = emptyMap()
            try {
                // Paso 1: geocodificar el destino con Nominatim
                val query = URLEncoder.encode(busqueda, "UTF-8")
                val nominatimUrl = "https://nominatim.openstreetmap.org/search?q=$query&format=json&limit=1"
                val nominatimResponse = withContext(Dispatchers.IO) {
                    val conn = java.net.URL(nominatimUrl).openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.setRequestProperty("User-Agent", "TripPlanner/1.0 Android")
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000
                    conn.connect()
                    conn.inputStream.bufferedReader().readText()
                }

                val nominatimJson = JSONArray(nominatimResponse)
                if (nominatimJson.length() == 0) {
                    error = "No se encontró el destino"
                    isLoading = false
                    return@launch
                }

                // Extraer coordenadas del primer resultado
                val lugar = nominatimJson.getJSONObject(0)
                val lat = lugar.getDouble("lat")
                val lon = lugar.getDouble("lon")
                destino = lugar.getString("display_name").split(",").first().trim()

                // Paso 2: buscar puntos de interés en un radio de 20km con Overpass API
                val radio = 20000
                val overpassQuery = """
                    [out:json][timeout:30];
                    (
                        node["tourism"="museum"](around:$radio,$lat,$lon);
                        node["historic"="monument"](around:$radio,$lat,$lon);
                        node["historic"="castle"](around:$radio,$lat,$lon);
                        node["historic"="ruins"](around:$radio,$lat,$lon);
                        node["amenity"="restaurant"](around:$radio,$lat,$lon);
                        node["amenity"="cafe"](around:$radio,$lat,$lon);
                        node["leisure"="park"](around:$radio,$lat,$lon);
                    );
                    out body 100;
                """.trimIndent()

                val overpassUrl = "https://overpass-api.de/api/interpreter"
                val overpassResponse = withContext(Dispatchers.IO) {
                    val conn = java.net.URL(overpassUrl).openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("User-Agent", "TripPlanner/1.0 Android")
                    conn.doOutput = true
                    conn.connectTimeout = 30000
                    conn.readTimeout = 30000
                    conn.outputStream.write("data=${URLEncoder.encode(overpassQuery, "UTF-8")}".toByteArray())
                    conn.inputStream.bufferedReader().readText()
                }

                // Paso 3: parsear y agrupar los resultados por categoría
                val overpassJson = JSONObject(overpassResponse)
                val elements = overpassJson.getJSONArray("elements")
                val resultado = mutableMapOf<String, MutableList<PuntoInteres>>()

                for (i in 0 until elements.length()) {
                    val element = elements.getJSONObject(i)
                    if (!element.has("tags")) continue
                    val tags = element.getJSONObject("tags")

                    // Ignorar lugares sin nombre en ningún idioma
                    val nombre = tags.optString("name", tags.optString("name:es", "")).ifBlank { continue }

                    // Determinar categoría y emoji según las etiquetas OSM del elemento
                    val (categoria, emoji) = when {
                        tags.has("tourism") && tags.getString("tourism") == "museum" -> "🏛️ Museos" to "🏛️"
                        tags.has("historic") && tags.getString("historic") == "monument" -> "🗿 Monumentos" to "🗿"
                        tags.has("historic") && tags.getString("historic") == "castle" -> "🏰 Castillos" to "🏰"
                        tags.has("historic") && tags.getString("historic") == "ruins" -> "🏚️ Ruinas" to "🏚️"
                        tags.has("amenity") && tags.getString("amenity") == "restaurant" -> "🍽️ Restaurantes" to "🍽️"
                        tags.has("amenity") && tags.getString("amenity") == "cafe" -> "☕ Cafés" to "☕"
                        tags.has("leisure") && tags.getString("leisure") == "park" -> "🌳 Parques" to "🌳"
                        else -> continue
                    }

                    if (!resultado.containsKey(categoria)) resultado[categoria] = mutableListOf()

                    // Limitar a 15 resultados por categoría para no saturar la pantalla
                    if ((resultado[categoria]?.size ?: 0) < 15) {
                        resultado[categoria]?.add(PuntoInteres(nombre, categoria, emoji))
                    }
                }

                puntos = resultado
                if (puntos.isEmpty()) error = "No se encontraron lugares de interés en este destino"

            } catch (e: Exception) {
                android.util.Log.e("Explorar", "Error: ${e.message}")
                error = "Error al buscar. Comprueba tu conexión."
            }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "Explorar",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Descubre qué ver en tu próximo destino",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Campo de búsqueda con teclado de tipo Search para mostrar el botón de lupa
        OutlinedTextField(
            value = busqueda,
            onValueChange = { busqueda = it },
            label = { Text("Busca un país o ciudad") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            trailingIcon = {
                IconButton(onClick = {
                    keyboardController?.hide()
                    buscarDestino()
                }) {
                    Icon(Icons.Default.Search, contentDescription = "Buscar", tint = TripBlue)
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                keyboardController?.hide()
                buscarDestino()
            }),
            colors = tripTextFieldColors()
        )

        Spacer(modifier = Modifier.height(16.dp))

        when {
            // Estado de carga mientras se consultan las APIs
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = TripBlue)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Buscando lugares de interés...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            // Estado de error si la búsqueda falló
            error.isNotEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = error, color = MaterialTheme.colorScheme.error)
                }
            }
            // Estado vacío antes de realizar la primera búsqueda
            puntos.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "✈️", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "¿A dónde quieres ir?",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Ej: Roma, París, Tokio, España...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            // Resultados agrupados por categoría
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "📍 $destino",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TripBlue
                        )
                    }

                    // Una tarjeta por categoría con la lista de puntos de interés
                    puntos.entries.forEach { (categoria, lista) ->
                        item {
                            Text(
                                text = categoria,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TripTextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    lista.forEachIndexed { index, punto ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        ) {
                                            Text(text = punto.emoji, fontSize = 20.sp)
                                            Text(
                                                text = punto.nombre,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        // Separador entre elementos excepto el último
                                        if (index < lista.size - 1) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}