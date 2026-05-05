package dam.pmdm.tripplanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dam.pmdm.tripplanner.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URLEncoder

data class LugarInteres(
    val nombre: String,
    val tipo: String,
    val direccion: String,
    val lat: Double,
    val lon: Double
)

@Composable
fun ExplorarScreen() {
    var busqueda by remember { mutableStateOf("") }
    var lugares by remember { mutableStateOf<List<LugarInteres>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    fun buscarLugares() {
        if (busqueda.isBlank()) return
        scope.launch {
            isLoading = true
            error = ""
            lugares = emptyList()
            try {
                val query = URLEncoder.encode(busqueda, "UTF-8")
                val url = "https://nominatim.openstreetmap.org/search?q=$query&format=json&limit=15&addressdetails=1"

                val response = withContext(Dispatchers.IO) {
                    val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("User-Agent", "TripPlanner Android App")
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    connection.connect()
                    connection.inputStream.bufferedReader().readText()
                }

                android.util.Log.d("Explorar", "Response: ${response.take(200)}")
                val jsonArray = JSONArray(response)
                val lista = mutableListOf<LugarInteres>()

                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val nombre = item.getString("display_name").split(",").first().trim()
                    val tipo = item.optString("type", "lugar").replaceFirstChar { it.uppercase() }
                    val direccion = item.getString("display_name")
                    val lat = item.getDouble("lat")
                    val lon = item.getDouble("lon")

                    lista.add(LugarInteres(nombre, tipo, direccion, lat, lon))
                }
                lugares = lista
            } catch (e: Exception) {
                android.util.Log.e("Explorar", "Error: ${e.message}")
                error = "Error al buscar: ${e.message}"
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
            text = "Descubre monumentos y lugares de interés",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = busqueda,
            onValueChange = { busqueda = it },
            label = { Text("Busca un lugar, ciudad o monumento") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            trailingIcon = {
                IconButton(onClick = {
                    keyboardController?.hide()
                    buscarLugares()
                }) {
                    Icon(Icons.Default.Search, contentDescription = "Buscar", tint = TripBlue)
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                keyboardController?.hide()
                buscarLugares()
            }),
            colors = tripTextFieldColors()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TripBlue)
            }
        } else if (error.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }
        } else if (lugares.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🔍", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Busca un destino",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Ej: Coliseo Roma, Torre Eiffel...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(lugares) { lugar ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(TripBlue.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(text = "📍", fontSize = 20.sp)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = lugar.nombre,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(TripBlue.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = lugar.tipo,
                                            fontSize = 11.sp,
                                            color = TripBlue,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = TripGray,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = lugar.direccion,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}