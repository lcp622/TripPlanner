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

fun Context.hayConexion(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

@Composable
fun SinConexionBanner() {
    val context = LocalContext.current
    var hayConexion by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        hayConexion = context.hayConexion()
    }

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