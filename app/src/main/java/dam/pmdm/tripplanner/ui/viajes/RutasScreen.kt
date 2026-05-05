package dam.pmdm.tripplanner.ui.viajes

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import dam.pmdm.tripplanner.BuildConfig
import dam.pmdm.tripplanner.ui.theme.*

@Composable
fun RutasScreen(
    idViaje: String,
    paisDestino: String
) {
    val context = LocalContext.current
    val apiKey = BuildConfig.MAPTILER_API_KEY
    val styleUrl = "https://api.maptiler.com/maps/streets/style.json?key=$apiKey"
    android.util.Log.d("RutasScreen", "API Key: $apiKey")
    android.util.Log.d("RutasScreen", "Style URL: $styleUrl")

    var mostrarDialogoAñadir by remember { mutableStateOf(false) }
    var nombrePunto by remember { mutableStateOf("") }

    if (mostrarDialogoAñadir) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoAñadir = false },
            title = { Text("Añadir punto de interés", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Pulsa en el mapa para seleccionar la ubicación",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = nombrePunto,
                        onValueChange = { nombrePunto = it },
                        label = { Text("Nombre del lugar") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = tripTextFieldColors()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarDialogoAñadir = false
                        nombrePunto = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TripBlue)
                ) {
                    Text("Añadir")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarDialogoAñadir = false
                    nombrePunto = ""
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                MapLibre.getInstance(ctx)
                val mapView = MapView(ctx)
                mapView.onCreate(Bundle())
                mapView.getMapAsync { map ->
                    map.setStyle(styleUrl) { style ->
                        // Estilo cargado
                    }
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(40.4168, -3.7038))
                        .zoom(5.0)
                        .build()
                }
                mapView
            },
            modifier = Modifier.fillMaxSize()
        )

        FloatingActionButton(
            onClick = { mostrarDialogoAñadir = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = TripBlue
        ) {
            Icon(Icons.Default.Add, contentDescription = "Añadir punto")
        }
    }
}