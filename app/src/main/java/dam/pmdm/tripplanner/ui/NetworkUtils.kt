package dam.pmdm.tripplanner.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Extensión de [Context] que comprueba si el dispositivo tiene conexión a internet.
 * Usa [NetworkCapabilities] en lugar de [ConnectivityManager.activeNetworkInfo]
 * que está deprecated desde Android 10 (API 29).
 *
 * Se comprueba la capacidad [NetworkCapabilities.NET_CAPABILITY_INTERNET] en lugar
 * de simplemente verificar si hay red activa, ya que una red puede estar activa
 * pero sin acceso real a internet (por ejemplo, una WiFi con portal cautivo).
 *
 * @return true si hay red activa con capacidad de internet, false en caso contrario
 */
fun Context.hayConexion(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    // Si no hay red activa, no hay conexión
    val network = cm.activeNetwork ?: return false
    // Comprobar las capacidades de la red activa
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

/**
 * Banner rojo que se muestra en la parte superior de la pantalla cuando
 * el dispositivo no tiene conexión a internet.
 *
 * Informa al usuario de que los datos mostrados provienen de la caché local
 * de Room y pueden no estar actualizados respecto a Firestore.
 *
 * Se comprueba la conexión una sola vez al montarse el composable con
 * [LaunchedEffect]. No escucha cambios en tiempo real de la conectividad
 * por simplicidad — el usuario puede recargar la pantalla si recupera conexión.
 *
 * Si hay conexión el composable no renderiza nada ([Unit]).
 */
@Composable
fun SinConexionBanner() {
    val context = LocalContext.current

    /** Estado de conectividad comprobado al montar el composable */
    var hayConexion by remember { mutableStateOf(true) }

    // Comprobar la conexión una sola vez al entrar en la pantalla
    LaunchedEffect(Unit) {
        hayConexion = context.hayConexion()
    }

    // Solo mostrar el banner si no hay conexión
    if (!hayConexion) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFF5252))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⚠️ Sin conexión — mostrando datos locales",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}