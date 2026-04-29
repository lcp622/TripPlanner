package dam.pmdm.tripplanner.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dam.pmdm.tripplanner.ui.perfil.PerfilScreen
import dam.pmdm.tripplanner.ui.theme.TripBlue
import dam.pmdm.tripplanner.ui.viajes.ViajeViewModel
import dam.pmdm.tripplanner.ui.viajes.ViajeViewModelFactory
import dam.pmdm.tripplanner.data.local.TripPlannerDatabase
import dam.pmdm.tripplanner.data.repository.FirestoreViajeRepository
import androidx.compose.ui.platform.LocalContext
import dam.pmdm.tripplanner.ui.viajes.ViajesScreen
import dam.pmdm.tripplanner.ui.gastos.GastosScreen

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun MainScreen(
    settingsViewModel: SettingsViewModel,
    onCerrarSesion: () -> Unit,
    onNuevoViaje: () -> Unit,
    onViajeClick: (String) -> Unit,
    onAjustes: () -> Unit
) {
    val items = listOf(
        BottomNavItem("Viajes", Icons.Default.Map, "viajes"),
        BottomNavItem("Explorar", Icons.Default.Explore, "explorar"),
        BottomNavItem("Gastos", Icons.Default.AttachMoney, "gastos"),
        BottomNavItem("Perfil", Icons.Default.Person, "perfil")
    )

    var selectedItem by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val db = TripPlannerDatabase.getInstance(context)
    val viajeRepository = FirestoreViajeRepository(db.viajeDao())
    val viajeViewModel: ViajeViewModel = viewModel(factory = ViajeViewModelFactory(viajeRepository))

    LaunchedEffect(Unit) {
        viajeViewModel.recargarViajes()
    }

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
        when (selectedItem) {
            0 -> ViajesScreen(
                viewModel = viajeViewModel,
                onNuevoViaje = onNuevoViaje,
                onViajeClick = onViajeClick,
                onAjustes = { selectedItem = 3 }
            )
            1 -> ExplorarScreen()
            2 -> GastosScreen()
            3 -> PerfilScreen(
                settingsViewModel = settingsViewModel,
                onCerrarSesion = onCerrarSesion
            )
        }
    }
}