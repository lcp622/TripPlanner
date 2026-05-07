package dam.pmdm.tripplanner.ui.viajes

import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.layout.*
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.JsonPrimitive
import dam.pmdm.tripplanner.BuildConfig
import dam.pmdm.tripplanner.data.local.TripPlannerDatabase
import dam.pmdm.tripplanner.data.local.entity.PuntoInteresEntity
import dam.pmdm.tripplanner.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.plugins.annotation.Symbol
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions
import java.util.UUID
import androidx.compose.ui.platform.LocalLocale
import org.maplibre.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RutasScreen(
    idViaje: String
) {
    val context = LocalContext.current
    val apiKey = BuildConfig.MAPTILER_API_KEY
    val styleUrl = "https://api.maptiler.com/maps/streets/style.json?key=$apiKey"

    val mostrarDialogoAnadir = remember { mutableStateOf(false) }
    val nombrePunto = remember { mutableStateOf("") }
    val descripcionPunto = remember { mutableStateOf("") }
    val categoriaSeleccionada = remember { mutableStateOf("ATRACCION") }
    val expandedCategoria = remember { mutableStateOf(false) }
    val latitudSeleccionada = remember { mutableDoubleStateOf(0.0) }
    val longitudSeleccionada = remember { mutableDoubleStateOf(0.0) }
    val contadorPuntos = remember { mutableIntStateOf(0) }
    val error = remember { mutableStateOf("") }
    val symbolManagerRef = remember { mutableStateOf<SymbolManager?>(null) }
    val symbolSeleccionado = remember { mutableStateOf<Symbol?>(null) }
    val nombreSeleccionado = remember { mutableStateOf("") }

    val categorias = listOf("ATRACCION", "RESTAURANTE", "HOTEL", "MUSEO", "MONUMENTO", "OTRO")

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(Bundle())
            onStart()
            onResume()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                symbolManagerRef.value?.onDestroy()
                mapView.onPause()
                mapView.onStop()
                mapView.onDestroy()
            } catch (e: Exception) {
                Log.e("RutasScreen", "Error al destruir mapa: ${e.message}")
            }
        }
    }

    symbolSeleccionado.value?.let { symbol ->
        AlertDialog(
            onDismissRequest = {
                symbolSeleccionado.value = null
                nombreSeleccionado.value = ""
            },
            title = { Text(nombreSeleccionado.value.ifBlank { "Punto de interés" }, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "¿Quieres eliminar este punto?",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        FirebaseFirestore.getInstance()
                            .collection("viajes").document(idViaje)
                            .collection("puntos_interes")
                            .whereEqualTo("nombre", nombreSeleccionado.value)
                            .get()
                            .addOnSuccessListener { snapshot ->
                                snapshot.documents.forEach { doc ->
                                    val idPunto = doc.getString("idPunto") ?: doc.id
                                    doc.reference.delete()
                                    CoroutineScope(Dispatchers.IO).launch {
                                        TripPlannerDatabase.getInstance(context)
                                            .puntoInteresDao()
                                            .eliminarPorId(idPunto)
                                    }
                                }
                            }
                        symbolManagerRef.value?.delete(symbol)
                        contadorPuntos.intValue--
                        symbolSeleccionado.value = null
                        nombreSeleccionado.value = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    symbolSeleccionado.value = null
                    nombreSeleccionado.value = ""
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (mostrarDialogoAnadir.value) {
        AlertDialog(
            onDismissRequest = {
                mostrarDialogoAnadir.value = false
                nombrePunto.value = ""
                descripcionPunto.value = ""
                error.value = ""
            },
            title = { Text("Añadir punto de interés", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Coordenadas: ${String.format(LocalLocale.current.platformLocale, "%.4f", latitudSeleccionada.doubleValue)}, ${String.format(LocalLocale.current.platformLocale, "%.4f", longitudSeleccionada.doubleValue)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = nombrePunto.value,
                        onValueChange = { nombrePunto.value = it },
                        label = { Text("Nombre del lugar *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = tripTextFieldColors()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = expandedCategoria.value,
                        onExpandedChange = { expandedCategoria.value = it }
                    ) {
                        OutlinedTextField(
                            value = categoriaSeleccionada.value,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Categoría") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoria.value) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                            shape = MaterialTheme.shapes.medium,
                            colors = tripTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCategoria.value,
                            onDismissRequest = { expandedCategoria.value = false }
                        ) {
                            categorias.forEach { categoria ->
                                DropdownMenuItem(
                                    text = { Text(categoria) },
                                    onClick = {
                                        categoriaSeleccionada.value = categoria
                                        expandedCategoria.value = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = descripcionPunto.value,
                        onValueChange = { descripcionPunto.value = it },
                        label = { Text("Descripción") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = tripTextFieldColors()
                    )
                    if (error.value.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = error.value, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nombrePunto.value.isBlank()) {
                            error.value = "El nombre es obligatorio"
                        } else {
                            val idPunto = UUID.randomUUID().toString()
                            val orden = contadorPuntos.intValue + 1
                            val nombre = nombrePunto.value

                            val puntoFirestore = mapOf(
                                "idPunto" to idPunto,
                                "nombre" to nombre,
                                "categoria" to categoriaSeleccionada.value,
                                "descripcion" to descripcionPunto.value.ifBlank { null },
                                "latitud" to latitudSeleccionada.doubleValue,
                                "longitud" to longitudSeleccionada.doubleValue,
                                "orden" to orden
                            )

                            FirebaseFirestore.getInstance()
                                .collection("viajes").document(idViaje)
                                .collection("puntos_interes")
                                .add(puntoFirestore)

                            CoroutineScope(Dispatchers.IO).launch {
                                TripPlannerDatabase.getInstance(context)
                                    .puntoInteresDao()
                                    .insertar(
                                        PuntoInteresEntity(
                                            idPunto = idPunto,
                                            idViaje = idViaje,
                                            nombre = nombre,
                                            categoria = categoriaSeleccionada.value,
                                            descripcion = descripcionPunto.value.ifBlank { null },
                                            latitud = latitudSeleccionada.doubleValue,
                                            longitud = longitudSeleccionada.doubleValue,
                                            orden = orden
                                        )
                                    )
                            }

                            symbolManagerRef.value?.create(
                                SymbolOptions()
                                    .withLatLng(LatLng(latitudSeleccionada.doubleValue, longitudSeleccionada.doubleValue))
                                    .withIconImage("marcador")
                                    .withIconSize(1.0f)
                                    .withData(JsonPrimitive(nombre))
                            )

                            contadorPuntos.intValue++
                            mostrarDialogoAnadir.value = false
                            nombrePunto.value = ""
                            descripcionPunto.value = ""
                            error.value = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TripBlue)
                ) {
                    Text("Añadir")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarDialogoAnadir.value = false
                    nombrePunto.value = ""
                    descripcionPunto.value = ""
                    error.value = ""
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                mapView.getMapAsync { map ->
                    map.setStyle(styleUrl) { style ->
                        val bitmap = android.graphics.BitmapFactory.decodeResource(
                            context.resources,
                            R.drawable.maplibre_marker_icon_default
                        )
                        if (bitmap != null) {
                            style.addImage("marcador", bitmap)
                        }

                        val symbolManager = SymbolManager(mapView, map, style)
                        symbolManager.iconAllowOverlap = true
                        symbolManager.textAllowOverlap = true
                        symbolManagerRef.value = symbolManager

                        symbolManager.addClickListener { symbol ->
                            val nombre = symbol.data?.asString ?: "Punto de interés"
                            nombreSeleccionado.value = nombre
                            symbolSeleccionado.value = symbol
                            true
                        }

                        map.addOnMapLongClickListener { point ->
                            latitudSeleccionada.doubleValue = point.latitude
                            longitudSeleccionada.doubleValue = point.longitude
                            mostrarDialogoAnadir.value = true
                            true
                        }

                        FirebaseFirestore.getInstance()
                            .collection("viajes").document(idViaje)
                            .collection("puntos_interes")
                            .get()
                            .addOnSuccessListener { snapshot ->
                                contadorPuntos.intValue = snapshot.size()
                                snapshot.documents.forEach { doc ->
                                    val lat = doc.getDouble("latitud") ?: return@forEach
                                    val lng = doc.getDouble("longitud") ?: return@forEach
                                    val nombre = doc.getString("nombre") ?: ""
                                    val categoria = doc.getString("categoria") ?: ""
                                    val descripcion = doc.getString("descripcion")
                                    val idPunto = doc.getString("idPunto") ?: doc.id
                                    val orden = doc.getLong("orden")?.toInt() ?: 0

                                    CoroutineScope(Dispatchers.IO).launch {
                                        TripPlannerDatabase.getInstance(context)
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


                                    symbolManager.create(
                                        SymbolOptions()
                                            .withLatLng(LatLng(lat, lng))
                                            .withIconImage("marcador")
                                            .withIconSize(1.0f)
                                            .withData(JsonPrimitive(nombre))
                                    )
                                }
                            }
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

        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = TripBlue.copy(alpha = 0.9f)
            )
        ) {
            Text(
                text = "📍 Mantén pulsado para añadir · Pulsa un marcador para eliminar",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }
}