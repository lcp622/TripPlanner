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
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.JsonObject
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

/**
 * Pantalla de rutas y puntos de interés de un viaje.
 * Muestra un mapa interactivo de MapLibre con los puntos de interés
 * del viaje marcados con iconos. Permite añadir y eliminar puntos.
 *
 * Interacción con el mapa:
 * - **Mantener pulsado** en el mapa → abre el diálogo para añadir un punto
 * - **Pulsar un marcador** → abre el diálogo para ver detalles y eliminar el punto
 *
 * Los puntos de interés se sincronizan entre Firestore y Room:
 * - Al cargar la pantalla se obtienen de Firestore y se cachean en Room
 * - Al añadir un punto se guarda en Firestore y Room simultáneamente
 * - Al eliminar un punto se borra de Firestore y Room simultáneamente
 *
 * Se usa [SymbolManager] del plugin de anotaciones de MapLibre en lugar
 * de los Markers deprecated para gestionar los iconos en el mapa.
 * Los datos de cada símbolo (nombre, categoría, descripción) se almacenan
 * como [JsonObject] en el campo data del símbolo para recuperarlos al pulsar.
 *
 * @param idViaje Identificador del viaje cuyos puntos de interés se muestran
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RutasScreen(
    idViaje: String
) {
    val context = LocalContext.current
    val apiKey = BuildConfig.MAPTILER_API_KEY
    val styleUrl = "https://api.maptiler.com/maps/streets/style.json?key=$apiKey"

    /** Controla la visibilidad del diálogo para añadir un nuevo punto */
    val mostrarDialogoAnadir = remember { mutableStateOf(false) }
    val nombrePunto = remember { mutableStateOf("") }
    val descripcionPunto = remember { mutableStateOf("") }
    val categoriaSeleccionada = remember { mutableStateOf("ATRACCION") }
    val expandedCategoria = remember { mutableStateOf(false) }

    /** Coordenadas del punto pulsado en el mapa para añadir un nuevo marcador */
    val latitudSeleccionada = remember { mutableDoubleStateOf(0.0) }
    val longitudSeleccionada = remember { mutableDoubleStateOf(0.0) }

    /** Contador de puntos para calcular el orden de cada nuevo punto */
    val contadorPuntos = remember { mutableIntStateOf(0) }
    val error = remember { mutableStateOf("") }

    /** Referencia al SymbolManager para crear y eliminar marcadores en el mapa */
    val symbolManagerRef = remember { mutableStateOf<SymbolManager?>(null) }

    /** Símbolo seleccionado al pulsar un marcador — activa el diálogo de eliminación */
    val symbolSeleccionado = remember { mutableStateOf<Symbol?>(null) }

    /** Datos del símbolo seleccionado para mostrar en el diálogo de eliminación */
    val nombreSeleccionado = remember { mutableStateOf("") }
    val categoriaSeleccionadaDialogo = remember { mutableStateOf("") }
    val descripcionSeleccionada = remember { mutableStateOf("") }

    /**
     * Bitmap del icono de marcador cargado fuera del AndroidView para evitar
     * el warning de LocalContext dentro de lambdas no-Composable.
     * Se carga una vez con remember para no recrearlo en cada recomposición.
     */
    val marcadorBitmap = remember {
        val drawable = ContextCompat.getDrawable(context, org.maplibre.android.R.drawable.maplibre_marker_icon_default)
        drawable?.toBitmap()
    }

    val categorias = listOf("ATRACCION", "RESTAURANTE", "HOTEL", "MUSEO", "MONUMENTO", "OTRO")

    /**
     * MapView inicializado una sola vez con remember para evitar recrearlo
     * en cada recomposición y perder el estado del mapa.
     * Se llama manualmente al ciclo de vida para que funcione fuera de un Fragment.
     */
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(Bundle())
            onStart()
            onResume()
        }
    }

    // Gestionar el ciclo de vida del mapa al salir de la pantalla
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

    // Diálogo de eliminación — se muestra al pulsar un marcador en el mapa
    symbolSeleccionado.value?.let { symbol ->
        AlertDialog(
            onDismissRequest = {
                symbolSeleccionado.value = null
                nombreSeleccionado.value = ""
                categoriaSeleccionadaDialogo.value = ""
                descripcionSeleccionada.value = ""
            },
            title = { Text(nombreSeleccionado.value.ifBlank { "Punto de interés" }, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    // Mostrar categoría y descripción si están disponibles
                    if (categoriaSeleccionadaDialogo.value.isNotBlank()) {
                        Text(
                            text = categoriaSeleccionadaDialogo.value,
                            color = TripBlue,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                    if (descripcionSeleccionada.value.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = descripcionSeleccionada.value,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
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
                        // Buscar y eliminar el punto en Firestore por nombre
                        FirebaseFirestore.getInstance()
                            .collection("viajes").document(idViaje)
                            .collection("puntos_interes")
                            .whereEqualTo("nombre", nombreSeleccionado.value)
                            .get()
                            .addOnSuccessListener { snapshot ->
                                snapshot.documents.forEach { doc ->
                                    val idPunto = doc.getString("idPunto") ?: doc.id
                                    doc.reference.delete()
                                    // Eliminar también de Room para mantener sincronía
                                    CoroutineScope(Dispatchers.IO).launch {
                                        TripPlannerDatabase.getInstance(context)
                                            .puntoInteresDao()
                                            .eliminarPorId(idPunto)
                                    }
                                }
                            }
                        // Eliminar el marcador del mapa
                        symbolManagerRef.value?.delete(symbol)
                        contadorPuntos.intValue--
                        symbolSeleccionado.value = null
                        nombreSeleccionado.value = ""
                        categoriaSeleccionadaDialogo.value = ""
                        descripcionSeleccionada.value = ""
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
                    categoriaSeleccionadaDialogo.value = ""
                    descripcionSeleccionada.value = ""
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo para añadir un nuevo punto de interés — se muestra al mantener pulsado el mapa
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
                    // Mostrar las coordenadas del punto pulsado para referencia
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
                    // Selector de categoría del punto de interés
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
                            val categoria = categoriaSeleccionada.value
                            val descripcion = descripcionPunto.value.ifBlank { null }

                            val puntoFirestore = mapOf(
                                "idPunto" to idPunto,
                                "nombre" to nombre,
                                "categoria" to categoria,
                                "descripcion" to descripcion,
                                "latitud" to latitudSeleccionada.doubleValue,
                                "longitud" to longitudSeleccionada.doubleValue,
                                "orden" to orden
                            )

                            // Guardar en Firestore para sincronización entre participantes
                            FirebaseFirestore.getInstance()
                                .collection("viajes").document(idViaje)
                                .collection("puntos_interes")
                                .add(puntoFirestore)

                            // Cachear en Room para acceso offline
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
                                            latitud = latitudSeleccionada.doubleValue,
                                            longitud = longitudSeleccionada.doubleValue,
                                            orden = orden
                                        )
                                    )
                            }

                            // Almacenar datos en el símbolo como JsonObject para recuperarlos al pulsar
                            val data = JsonObject().apply {
                                addProperty("nombre", nombre)
                                addProperty("categoria", categoria)
                                addProperty("descripcion", descripcion ?: "")
                            }

                            symbolManagerRef.value?.create(
                                SymbolOptions()
                                    .withLatLng(LatLng(latitudSeleccionada.doubleValue, longitudSeleccionada.doubleValue))
                                    .withIconImage("marcador")
                                    .withIconSize(1.0f)
                                    .withData(data)
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
                        // Registrar el icono del marcador en el estilo del mapa
                        if (marcadorBitmap != null) {
                            style.addImage("marcador", marcadorBitmap)
                        }

                        val symbolManager = SymbolManager(mapView, map, style)
                        symbolManager.iconAllowOverlap = true
                        symbolManager.textAllowOverlap = true
                        symbolManagerRef.value = symbolManager

                        // Click en marcador: extraer datos del JsonObject y mostrar diálogo
                        symbolManager.addClickListener { symbol ->
                            val obj = symbol.data?.asJsonObject
                            nombreSeleccionado.value = obj?.get("nombre")?.asString ?: "Punto de interés"
                            categoriaSeleccionadaDialogo.value = obj?.get("categoria")?.asString ?: ""
                            descripcionSeleccionada.value = obj?.get("descripcion")?.asString ?: ""
                            symbolSeleccionado.value = symbol
                            true
                        }

                        // Long click en el mapa: guardar coordenadas y mostrar diálogo de añadir
                        map.addOnMapLongClickListener { point ->
                            latitudSeleccionada.doubleValue = point.latitude
                            longitudSeleccionada.doubleValue = point.longitude
                            mostrarDialogoAnadir.value = true
                            true
                        }

                        // Cargar puntos existentes desde Firestore al iniciar la pantalla
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
                                    val descripcion = doc.getString("descripcion") ?: ""
                                    val idPunto = doc.getString("idPunto") ?: doc.id
                                    val orden = doc.getLong("orden")?.toInt() ?: 0

                                    // Cachear en Room para acceso offline
                                    CoroutineScope(Dispatchers.IO).launch {
                                        TripPlannerDatabase.getInstance(context)
                                            .puntoInteresDao()
                                            .insertar(
                                                PuntoInteresEntity(
                                                    idPunto = idPunto,
                                                    idViaje = idViaje,
                                                    nombre = nombre,
                                                    categoria = categoria,
                                                    descripcion = descripcion.ifBlank { null },
                                                    latitud = lat,
                                                    longitud = lng,
                                                    orden = orden
                                                )
                                            )
                                    }

                                    // Crear marcador en el mapa con los datos del punto
                                    val data = JsonObject().apply {
                                        addProperty("nombre", nombre)
                                        addProperty("categoria", categoria)
                                        addProperty("descripcion", descripcion)
                                    }

                                    symbolManager.create(
                                        SymbolOptions()
                                            .withLatLng(LatLng(lat, lng))
                                            .withIconImage("marcador")
                                            .withIconSize(1.0f)
                                            .withData(data)
                                    )
                                }
                            }
                    }

                    // Centrar el mapa en España al iniciar
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(40.4168, -3.7038))
                        .zoom(5.0)
                        .build()
                }
                mapView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Banner de instrucciones de uso del mapa
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