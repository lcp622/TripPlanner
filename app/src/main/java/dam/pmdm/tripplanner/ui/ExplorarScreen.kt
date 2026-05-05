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

data class PuntoInteres(
    val nombre: String,
    val tipo: String,
    val emoji: String
)

@Composable
fun ExplorarScreen() {
    var busqueda by remember { mutableStateOf("") }
    var destino by remember { mutableStateOf("") }
    var puntos by remember { mutableStateOf<Map<String, List<PuntoInteres>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    fun buscarDestino() {
        if (busqueda.isBlank()) return
        scope.launch {
            isLoading = true
            error = ""
            puntos = emptyMap()
            try {
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

                val lugar = nominatimJson.getJSONObject(0)
                val lat = lugar.getDouble("lat")
                val lon = lugar.getDouble("lon")
                destino = lugar.getString("display_name").split(",").first().trim()

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

                val overpassJson = JSONObject(overpassResponse)
                val elements = overpassJson.getJSONArray("elements")
                val resultado = mutableMapOf<String, MutableList<PuntoInteres>>()

                for (i in 0 until elements.length()) {
                    val element = elements.getJSONObject(i)
                    if (!element.has("tags")) continue
                    val tags = element.getJSONObject("tags")
                    val nombre = tags.optString("name", tags.optString("name:es", "")).ifBlank { continue }

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

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = TripBlue)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Buscando lugares de interés...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else if (error.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }
        } else if (puntos.isEmpty()) {
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
        } else {
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