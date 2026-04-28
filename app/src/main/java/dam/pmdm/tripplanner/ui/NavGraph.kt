package dam.pmdm.tripplanner.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import dam.pmdm.tripplanner.data.local.TripPlannerDatabase
import dam.pmdm.tripplanner.data.local.entity.UsuarioEntity
import dam.pmdm.tripplanner.data.local.entity.ViajeEntity
import dam.pmdm.tripplanner.data.repository.ActividadRepository
import dam.pmdm.tripplanner.data.repository.FirestoreViajeRepository
import dam.pmdm.tripplanner.ui.auth.AuthViewModel
import dam.pmdm.tripplanner.ui.auth.LoginScreen
import dam.pmdm.tripplanner.ui.auth.RegisterScreen
import dam.pmdm.tripplanner.ui.itinerario.ActividadViewModel
import dam.pmdm.tripplanner.ui.itinerario.ActividadViewModelFactory
import dam.pmdm.tripplanner.ui.itinerario.CrearActividadScreen
import dam.pmdm.tripplanner.ui.perfil.SettingsScreen
import dam.pmdm.tripplanner.ui.viajes.CrearViajeScreen
import dam.pmdm.tripplanner.ui.viajes.DetalleViajeScreen
import dam.pmdm.tripplanner.ui.viajes.ViajeViewModel
import dam.pmdm.tripplanner.ui.viajes.ViajeViewModelFactory
import dam.pmdm.tripplanner.ui.viajes.ViajesScreen
import kotlinx.coroutines.launch

object Rutas {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val VIAJES = "viajes"
    const val CREAR_VIAJE = "crear_viaje"
    const val DETALLE_VIAJE = "detalle_viaje/{idViaje}"
    const val CREAR_ACTIVIDAD = "crear_actividad/{idViaje}"
    const val SETTINGS = "settings"
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel(),
    settingsViewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val db = TripPlannerDatabase.getInstance(context)
    val scope = rememberCoroutineScope()

    val viajeRepository = FirestoreViajeRepository(db.viajeDao())
    val viajeViewModel: ViajeViewModel = viewModel(factory = ViajeViewModelFactory(viajeRepository))

    val actividadRepository = ActividadRepository(db.actividadDao())
    val actividadViewModel: ActividadViewModel = viewModel(factory = ActividadViewModelFactory(actividadRepository))

    LaunchedEffect(Unit) {
        scope.launch {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                val usuario = UsuarioEntity(
                    idUsuario = user.uid,
                    nombre = user.displayName ?: user.email ?: "Usuario",
                    email = user.email ?: ""
                )
                db.usuarioDao().insertar(usuario)
            }
        }
    }

    val startDestination = if (authViewModel.estaAutenticado) Rutas.VIAJES else Rutas.LOGIN

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Rutas.LOGIN) {
            LoginScreen(
                onLoginExitoso = {
                    navController.navigate(Rutas.VIAJES) {
                        popUpTo(Rutas.LOGIN) { inclusive = true }
                    }
                },
                onIrARegistro = { navController.navigate(Rutas.REGISTER) }
            )
        }

        composable(Rutas.REGISTER) {
            RegisterScreen(
                onRegistroExitoso = {
                    navController.navigate(Rutas.VIAJES) {
                        popUpTo(Rutas.LOGIN) { inclusive = true }
                    }
                },
                onVolverALogin = { navController.popBackStack() }
            )
        }

        composable(Rutas.VIAJES) {
            LaunchedEffect(Unit) {
                viajeViewModel.recargarViajes()
            }
            ViajesScreen(
                viewModel = viajeViewModel,
                onNuevoViaje = { navController.navigate(Rutas.CREAR_VIAJE) },
                onViajeClick = { idViaje ->
                    navController.navigate("detalle_viaje/$idViaje")
                },
                onAjustes = { navController.navigate(Rutas.SETTINGS) }
            )
        }

        composable(Rutas.CREAR_VIAJE) {
            CrearViajeScreen(
                viewModel = viajeViewModel,
                onViajeCreado = { navController.popBackStack() },
                onVolver = { navController.popBackStack() }
            )
        }

        composable(Rutas.DETALLE_VIAJE) { backStackEntry ->
            val idViaje = backStackEntry.arguments?.getString("idViaje") ?: ""
            var viaje by remember(idViaje) { mutableStateOf<ViajeEntity?>(null) }

            LaunchedEffect(idViaje) {
                viaje = viajeRepository.obtenerViajePorIdFirestore(idViaje)
            }

            viaje?.let { v ->
                DetalleViajeScreen(
                    viaje = v,
                    actividadViewModel = actividadViewModel,
                    onVolver = { navController.popBackStack() },
                    onNuevaActividad = {
                        navController.navigate("crear_actividad/$idViaje")
                    }
                )
            }
        }

        composable(Rutas.CREAR_ACTIVIDAD) { backStackEntry ->
            val idViaje = backStackEntry.arguments?.getString("idViaje") ?: ""
            CrearActividadScreen(
                idViaje = idViaje,
                viewModel = actividadViewModel,
                onActividadCreada = { navController.popBackStack() },
                onVolver = { navController.popBackStack() }
            )
        }

        composable(Rutas.SETTINGS) {
            SettingsScreen(
                settingsViewModel = settingsViewModel,
                onVolver = { navController.popBackStack() }
            )
        }
    }
}