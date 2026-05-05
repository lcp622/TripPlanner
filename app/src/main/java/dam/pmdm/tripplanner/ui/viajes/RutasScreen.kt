package dam.pmdm.tripplanner.ui.viajes

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.firestore.FirebaseFirestore
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import dam.pmdm.tripplanner.BuildConfig
import dam.pmdm.tripplanner.ui.theme.*
import kotlinx.coroutines.tasks.await

@Composable
fun RutasScreen(
    idViaje: String,
    paisDestino: String
) {
    val context = LocalContext.current
    val apiKey = BuildConfig.MAPTILER_API_KEY
    val styleUrl = "https://api.maptiler.com/maps/streets/style.json?key=$apiKey"

    var mostrarDialogoAñadir by remember { mutableStateOf(false) }
    var nombrePunto by remember { mutableStateOf("") }
    var latitudSeleccionada by remember { mutableStateOf(0.0) }
    var longitudSeleccionada by remember { mutableStateOf(0.0) }
    var error by remember { mutableStateOf("") }
    var mapRef by remember { mutableStateOf<org.maplibre.android.maps.MapLibreMap?>(null) }

    if (mostrarDialogoAñadir) {
        AlertDialog(
            onDismissRequest = {
                mostrarDialogoAñadir = false
                nombrePunto = ""
                error = ""
            },
            title = { Text("Añadir punto de interés", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Coordenadas: ${String.format("%.4f", latitudSeleccionada)}, ${String.format("%.4f", longitudSeleccionada)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = nombrePunto,
                        onValueChange = { nombrePunto = it },
                        label = { Text("Nombre del lugar *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = tripTextFieldColors()
                    )
                    if (error.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nombrePunto.isBlank()) {
                            error = "El nombre es obligatorio"
                        } else {
                            // Guardar en Firestore
                            val punto = mapOf(
                                "idPunto" to java.util.UUID.randomUUID().toString(),
                                "nombre" to nombrePunto,
                                "latitud" to latitudSeleccionada,
                                "longitud" to longitudSeleccionada
                            )
                            FirebaseFirestore.getInstance()
                                .collection("viajes").document(idViaje)
                                .collection("puntos_interes")
                                .add(punto)

                            // Añadir marcador al mapa
                            mapRef?.addMarker(
                                MarkerOptions()
                                    .position(LatLng(latitudSeleccionada, longitudSeleccionada))
                                    .title(nombrePunto)
                            )

                            mostrarDialogoAñadir = false
                            nombrePunto = ""
                            error = ""
                        }
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
                    error = ""
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
                    mapRef = map
                    map.setStyle(styleUrl) {
                        // Cargar puntos existentes de Firestore
                        FirebaseFirestore.getInstance()
                            .collection("viajes").document(idViaje)
                            .collection("puntos_interes")
                            .get()
                            .addOnSuccessListener { snapshot ->
                                snapshot.documents.forEach { doc ->
                                    val lat = doc.getDouble("latitud") ?: return@forEach
                                    val lng = doc.getDouble("longitud") ?: return@forEach
                                    val nombre = doc.getString("nombre") ?: ""
                                    map.addMarker(
                                        MarkerOptions()
                                            .position(LatLng(lat, lng))
                                            .title(nombre)
                                    )
                                }
                            }
                    }
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(40.4168, -3.7038))
                        .zoom(5.0)
                        .build()

                    // Detectar tap en el mapa
                    map.addOnMapClickListener { point ->
                        latitudSeleccionada = point.latitude
                        longitudSeleccionada = point.longitude
                        mostrarDialogoAñadir = true
                        true
                    }
                }
                mapView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Instrucción
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = TripBlue.copy(alpha = 0.9f)
            )
        ) {
            Text(
                text = "📍 Pulsa en el mapa para añadir un punto",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = androidx.compose.ui.graphics.Color.White,
                fontSize = 13.sp
            )
        }
    }
}