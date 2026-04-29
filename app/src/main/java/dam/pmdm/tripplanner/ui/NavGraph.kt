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
import dam.pmdm.tripplanner.ui.viajes.CrearViajeScreen
import dam.pmdm.tripplanner.ui.viajes.DetalleViajeScreen
import dam.pmdm.tripplanner.ui.viajes.EditarViajeScreen
import dam.pmdm.tripplanner.ui.viajes.ViajeViewModel
import dam.pmdm.tripplanner.ui.viajes.ViajeViewModelFactory
import kotlinx.coroutines.launch

object Rutas {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAIN = "main"
    const val CREAR_VIAJE = "crear_viaje"
    const val DETALLE_VIAJE = "detalle_viaje/{idViaje}"
    const val EDITAR_VIAJE = "editar_viaje/{idViaje}"
    const val CREAR_ACTIVIDAD = "crear_actividad/{idViaje}"
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

    val startDestination = if (authViewModel.estaAutenticado) Rutas.MAIN else Rutas.LOGIN

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Rutas.LOGIN) {
            LoginScreen(
                onLoginExitoso = {
                    navController.navigate(Rutas.MAIN) {
                        popUpTo(Rutas.LOGIN) { inclusive = true }
                    }
                },
                onIrARegistro = { navController.navigate(Rutas.REGISTER) }
            )
        }

        composable(Rutas.REGISTER) {
            RegisterScreen(
                onRegistroExitoso = {
                    navController.navigate(Rutas.MAIN) {
                        popUpTo(Rutas.LOGIN) { inclusive = true }
                    }
                },
                onVolverALogin = { navController.popBackStack() }
            )
        }

        composable(Rutas.MAIN) {
            MainScreen(
                settingsViewModel = settingsViewModel,
                onCerrarSesion = {
                    authViewModel.cerrarSesion()
                    navController.navigate(Rutas.LOGIN) {
                        popUpTo(Rutas.MAIN) { inclusive = true }
                    }
                },
                onNuevoViaje = { navController.navigate(Rutas.CREAR_VIAJE) },
                onViajeClick = { idViaje ->
                    navController.navigate("detalle_viaje/$idViaje")
                }
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
                    viajeViewModel = viajeViewModel,
                    onVolver = { navController.popBackStack() },
                    onNuevaActividad = {
                        navController.navigate("crear_actividad/$idViaje")
                    },
                    onViajeEliminado = { navController.popBackStack() },
                    onEditarViaje = {
                        navController.navigate("editar_viaje/$idViaje")
                    }
                )
            }
        }

        composable(Rutas.EDITAR_VIAJE) { backStackEntry ->
            val idViaje = backStackEntry.arguments?.getString("idViaje") ?: ""
            var viaje by remember(idViaje) { mutableStateOf<ViajeEntity?>(null) }

            LaunchedEffect(idViaje) {
                viaje = viajeRepository.obtenerViajePorIdFirestore(idViaje)
            }

            viaje?.let { v ->
                EditarViajeScreen(
                    viaje = v,
                    viewModel = viajeViewModel,
                    onViajeActualizado = { navController.popBackStack() },
                    onVolver = { navController.popBackStack() }
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
    }
}