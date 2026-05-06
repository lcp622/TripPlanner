package dam.pmdm.tripplanner.ui.viajes

import android.os.Bundle
import androidx.compose.foundation.layout.*
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
import dam.pmdm.tripplanner.BuildConfig
import dam.pmdm.tripplanner.data.local.TripPlannerDatabase
import dam.pmdm.tripplanner.data.local.entity.PuntoInteresEntity
import dam.pmdm.tripplanner.ui.theme.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView

@OptIn(ExperimentalMaterial3Api::class)
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
    var descripcionPunto by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf("ATRACCION") }
    var expandedCategoria by remember { mutableStateOf(false) }
    var latitudSeleccionada by remember { mutableStateOf(0.0) }
    var longitudSeleccionada by remember { mutableStateOf(0.0) }
    var contadorPuntos by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf("") }
    var mapRef by remember { mutableStateOf<org.maplibre.android.maps.MapLibreMap?>(null) }
    var marcadorSeleccionado by remember { mutableStateOf<Marker?>(null) }

    val categorias = listOf("ATRACCION", "RESTAURANTE", "HOTEL", "MUSEO", "MONUMENTO", "OTRO")

    // Diálogo opciones marcador seleccionado
    marcadorSeleccionado?.let { marker ->
        AlertDialog(
            onDismissRequest = { marcadorSeleccionado = null },
            title = { Text(marker.title ?: "Punto de interés", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (!marker.snippet.isNullOrBlank()) {
                        Text(
                            text = marker.snippet,
                            color = TripBlue,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "¿Qué quieres hacer con este punto?",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val nombreMarker = marker.title
                        FirebaseFirestore.getInstance()
                            .collection("viajes").document(idViaje)
                            .collection("puntos_interes")
                            .whereEqualTo("nombre", nombreMarker)
                            .get()
                            .addOnSuccessListener { snapshot ->
                                snapshot.documents.forEach { doc ->
                                    val idPunto = doc.getString("idPunto") ?: doc.id
                                    doc.reference.delete()
                                    GlobalScope.launch {
                                        TripPlannerDatabase.getInstance(context)
                                            .puntoInteresDao()
                                            .eliminarPorId(idPunto)
                                    }
                                }
                            }
                        mapRef?.removeMarker(marker)
                        contadorPuntos--
                        marcadorSeleccionado = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { marcadorSeleccionado = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo añadir punto
    if (mostrarDialogoAñadir) {
        AlertDialog(
            onDismissRequest = {
                mostrarDialogoAñadir = false
                nombrePunto = ""
                descripcionPunto = ""
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
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = expandedCategoria,
                        onExpandedChange = { expandedCategoria = it }
                    ) {
                        OutlinedTextField(
                            value = categoriaSeleccionada,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Categoría") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoria) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = MaterialTheme.shapes.medium,
                            colors = tripTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCategoria,
                            onDismissRequest = { expandedCategoria = false }
                        ) {
                            categorias.forEach { categoria ->
                                DropdownMenuItem(
                                    text = { Text(categoria) },
                                    onClick = {
                                        categoriaSeleccionada = categoria
                                        expandedCategoria = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = descripcionPunto,
                        onValueChange = { descripcionPunto = it },
                        label = { Text("Descripción") },
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
                            val idPunto = java.util.UUID.randomUUID().toString()
                            val orden = contadorPuntos + 1

                            val puntoFirestore = mapOf(
                                "idPunto" to idPunto,
                                "nombre" to nombrePunto,
                                "categoria" to categoriaSeleccionada,
                                "descripcion" to descripcionPunto.ifBlank { null },
                                "latitud" to latitudSeleccionada,
                                "longitud" to longitudSeleccionada,
                                "orden" to orden
                            )

                            FirebaseFirestore.getInstance()
                                .collection("viajes").document(idViaje)
                                .collection("puntos_interes")
                                .add(puntoFirestore)

                            GlobalScope.launch {
                                TripPlannerDatabase.getInstance(context)
                                    .puntoInteresDao()
                                    .insertar(
                                        PuntoInteresEntity(
                                            idPunto = idPunto,
                                            idViaje = idViaje,
                                            nombre = nombrePunto,
                                            categoria = categoriaSeleccionada,
                                            descripcion = descripcionPunto.ifBlank { null },
                                            latitud = latitudSeleccionada,
                                            longitud = longitudSeleccionada,
                                            orden = orden
                                        )
                                    )
                            }

                            mapRef?.addMarker(
                                MarkerOptions()
                                    .position(LatLng(latitudSeleccionada, longitudSeleccionada))
                                    .title(nombrePunto)
                                    .snippet(categoriaSeleccionada)
                            )

                            contadorPuntos++
                            mostrarDialogoAñadir = false
                            nombrePunto = ""
                            descripcionPunto = ""
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
                    descripcionPunto = ""
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
                mapView.onStart()
                mapView.onResume()
                mapView.getMapAsync { map ->
                    mapRef = map
                    map.setStyle(styleUrl) {
                        FirebaseFirestore.getInstance()
                            .collection("viajes").document(idViaje)
                            .collection("puntos_interes")
                            .get()
                            .addOnSuccessListener { snapshot ->
                                contadorPuntos = snapshot.size()
                                snapshot.documents.forEach { doc ->
                                    val lat = doc.getDouble("latitud") ?: return@forEach
                                    val lng = doc.getDouble("longitud") ?: return@forEach
                                    val nombre = doc.getString("nombre") ?: ""
                                    val categoria = doc.getString("categoria") ?: ""
                                    val descripcion = doc.getString("descripcion")
                                    val idPunto = doc.getString("idPunto") ?: doc.id
                                    val orden = doc.getLong("orden")?.toInt() ?: 0

                                    GlobalScope.launch {
                                        TripPlannerDatabase.getInstance(ctx)
                                            .puntoInteresDao()
                                            .insertar(
                                                PuntoInteresEntity(
                                                    idPunto = idPunto,
                                                    idViaje = idViaje,
                                                    nombre = nombre,
                                                    categoria = categoria,
                                                    descripcion = descripcion,
                                                    latitud = lat,
                                                    longitud = lng,
                                                    orden = orden
                                                )
                                            )
                                    }

                                    map.addMarker(
                                        MarkerOptions()
                                            .position(LatLng(lat, lng))
                                            .title(nombre)
                                            .snippet(categoria)
                                    )
                                }
                            }
                    }

                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(40.4168, -3.7038))
                        .zoom(5.0)
                        .build()

                    map.setOnMarkerClickListener { marker ->
                        marcadorSeleccionado = marker
                        true
                    }

                    map.addOnMapClickListener { point ->
                        latitudSeleccionada = point.latitude
                        longitudSeleccionada = point.longitude
                        mostrarDialogoAñadir = true
                        true
                    }
                }
                mapView
            },
            onRelease = { mapView ->
                mapView.onPause()
                mapView.onStop()
                mapView.onDestroy()
            },
            modifier = Modifier.fillMaxSize()
        )

        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = TripBlue.copy(alpha = 0.9f)
            )
        ) {
            Text(
                text = "📍 Pulsa en el mapa para añadir · Pulsa un marcador para eliminar",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = androidx.compose.ui.graphics.Color.White,
                fontSize = 12.sp
            )
        }
    }
}