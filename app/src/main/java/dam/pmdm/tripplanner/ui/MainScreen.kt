package dam.pmdm.tripplanner.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import dam.pmdm.tripplanner.data.local.TripPlannerDatabase
import dam.pmdm.tripplanner.data.repository.FirestoreViajeRepository
import dam.pmdm.tripplanner.ui.gastos.GastosScreen
import dam.pmdm.tripplanner.ui.perfil.PerfilScreen
import dam.pmdm.tripplanner.ui.theme.TripBlue
import dam.pmdm.tripplanner.ui.viajes.ViajeViewModel
import dam.pmdm.tripplanner.ui.viajes.ViajeViewModelFactory
import dam.pmdm.tripplanner.ui.viajes.ViajesScreen

/**
 * Modelo de datos para cada elemento de la barra de navegación inferior.
 *
 * @property label Texto visible bajo el icono
 * @property icon Icono que representa la sección
 * @property route Identificador de la ruta para la navegación
 */
data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

/**
 * Pantalla principal de la aplicación con navegación por pestañas.
 * Contiene cuatro secciones accesibles desde la barra inferior:
 * - **Viajes**: lista de viajes del usuario con opción de crear uno nuevo
 * - **Explorar**: pantalla de exploración de destinos
 * - **Gastos**: resumen global de gastos de todos los viajes
 * - **Perfil**: configuración de cuenta y preferencias
 *
 * Muestra el [SinConexionBanner] en la parte superior si no hay conexión,
 * informando al usuario de que los datos provienen de la caché local.
 *
 * Se crea un [ViajeViewModel] propio en esta pantalla en lugar de reutilizar
 * el del [NavGraph] para garantizar que los viajes se recargan siempre
 * al volver a la pantalla principal desde el detalle de un viaje.
 *
 * @param settingsViewModel ViewModel de ajustes para el modo oscuro en PerfilScreen
 * @param onCerrarSesion Callback que cierra sesión y navega al login
 * @param onNuevoViaje Callback que navega a la pantalla de crear viaje
 * @param onViajeClick Callback que navega al detalle de un viaje con su id
 * @param onEditarPerfil Callback que navega a la pantalla de editar perfil
 */
@Composable
fun MainScreen(
    settingsViewModel: SettingsViewModel,
    onCerrarSesion: () -> Unit,
    onNuevoViaje: () -> Unit,
    onViajeClick: (String) -> Unit,
    onEditarPerfil: () -> Unit
) {
    /** Elementos de la barra de navegación inferior */
    val items = listOf(
        BottomNavItem("Viajes", Icons.Default.Map, "viajes"),
        BottomNavItem("Explorar", Icons.Default.Explore, "explorar"),
        BottomNavItem("Gastos", Icons.Default.AttachMoney, "gastos"),
        BottomNavItem("Perfil", Icons.Default.Person, "perfil")
    )

    /** Índice de la pestaña seleccionada actualmente */
    var selectedItem by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val db = TripPlannerDatabase.getInstance(context)
    val viajeRepository = FirestoreViajeRepository(db.viajeDao())
    val viajeViewModel: ViajeViewModel = viewModel(factory = ViajeViewModelFactory(viajeRepository))

    // Recargar viajes al entrar en la pantalla principal para reflejar cambios
    LaunchedEffect(Unit) {
        viajeViewModel.recargarViajes()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Banner de aviso de sin conexión — solo visible si no hay internet
        SinConexionBanner()

        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedItem == index,
                            onClick = { selectedItem = index },
                            icon = {
                                Icon(item.icon, contentDescription = item.label)
                            },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TripBlue,
                                selectedTextColor = TripBlue,
                                indicatorColor = TripBlue.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        ) { padding ->
            // Mostrar la pantalla correspondiente a la pestaña seleccionada
            Box(modifier = Modifier.padding(padding)) {
                when (selectedItem) {
                    0 -> ViajesScreen(
                        viewModel = viajeViewModel,
                        onNuevoViaje = onNuevoViaje,
                        onViajeClick = onViajeClick
                    )
                    1 -> ExplorarScreen()
                    2 -> GastosScreen()
                    3 -> PerfilScreen(
                        settingsViewModel = settingsViewModel,
                        onCerrarSesion = onCerrarSesion,
                        onEditarPerfil = onEditarPerfil
                    )
                }
            }
        }
    }
}