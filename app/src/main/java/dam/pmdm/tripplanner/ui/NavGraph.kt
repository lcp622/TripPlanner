package dam.pmdm.tripplanner.ui


import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import dam.pmdm.tripplanner.data.repository.GastoRepository
import dam.pmdm.tripplanner.ui.auth.AuthViewModel
import dam.pmdm.tripplanner.ui.auth.LoginScreen
import dam.pmdm.tripplanner.ui.auth.RegisterScreen
import dam.pmdm.tripplanner.ui.gastos.CrearGastoScreen
import dam.pmdm.tripplanner.ui.gastos.EditarGastoScreen
import dam.pmdm.tripplanner.ui.gastos.GastoViewModel
import dam.pmdm.tripplanner.ui.gastos.GastoViewModelFactory
import dam.pmdm.tripplanner.ui.itinerario.ActividadViewModel
import dam.pmdm.tripplanner.ui.itinerario.ActividadViewModelFactory
import dam.pmdm.tripplanner.ui.itinerario.CrearActividadScreen
import dam.pmdm.tripplanner.ui.itinerario.EditarActividadScreen
import dam.pmdm.tripplanner.ui.perfil.EditarPerfilScreen
import dam.pmdm.tripplanner.ui.theme.TripBlue
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
    const val CREAR_GASTO = "crear_gasto/{idViaje}"
    const val EDITAR_ACTIVIDAD = "editar_actividad/{idViaje}/{idActividad}"
    const val EDITAR_GASTO = "editar_gasto/{idViaje}/{idGasto}"
    const val EDITAR_PERFIL = "editar_perfil"
}

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

    val gastoRepository = GastoRepository(db.gastoDao())
    val gastoViewModel: GastoViewModel = viewModel(factory = GastoViewModelFactory(gastoRepository))

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

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {

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
                },
                onEditarPerfil = { navController.navigate(Rutas.EDITAR_PERFIL) }
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

            if (viaje == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TripBlue)
                }
            } else {
                DetalleViajeScreen(
                    viaje = viaje!!,
                    actividadViewModel = actividadViewModel,
                    viajeViewModel = viajeViewModel,
                    gastoViewModel = gastoViewModel,
                    gastoRepository = gastoRepository,
                    viajeRepository = viajeRepository,
                    onVolver = { navController.popBackStack() },
                    onNuevaActividad = {
                        navController.navigate("crear_actividad/$idViaje")
                    },
                    onEditarActividad = { idActividad ->
                        navController.navigate("editar_actividad/$idViaje/$idActividad")
                    },
                    onNuevoGasto = {
                        navController.navigate("crear_gasto/$idViaje")
                    },
                    onViajeEliminado = { navController.popBackStack() },
                    onEditarViaje = {
                        navController.navigate("editar_viaje/$idViaje")
                    },
                    onEditarGasto = { idGasto ->
                        navController.navigate("editar_gasto/$idViaje/$idGasto")
                    }
                )
            }
        }

        composable(Rutas.EDITAR_ACTIVIDAD) { backStackEntry ->
            val idActividad = backStackEntry.arguments?.getString("idActividad") ?: ""
            var actividad by remember { mutableStateOf<dam.pmdm.tripplanner.data.local.entity.ActividadEntity?>(null) }

            LaunchedEffect(idActividad) {
                actividad = actividadRepository.obtenerPorId(idActividad)
            }

            actividad?.let { a ->
                EditarActividadScreen(
                    actividad = a,
                    viewModel = actividadViewModel,
                    onActividadActualizada = { navController.popBackStack() },
                    onVolver = { navController.popBackStack() }
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

        composable(Rutas.CREAR_GASTO) { backStackEntry ->
            val idViaje = backStackEntry.arguments?.getString("idViaje") ?: ""
            CrearGastoScreen(
                idViaje = idViaje,
                viewModel = gastoViewModel,
                onGastoCreado = { navController.popBackStack() },
                onVolver = { navController.popBackStack() }
            )
        }

        composable(Rutas.EDITAR_GASTO) { backStackEntry ->
            val idGasto = backStackEntry.arguments?.getString("idGasto") ?: ""
            var gasto by remember { mutableStateOf<dam.pmdm.tripplanner.data.local.entity.GastoEntity?>(null) }

            LaunchedEffect(idGasto) {
                gasto = db.gastoDao().obtenerPorId(idGasto)
            }

            gasto?.let { g ->
                EditarGastoScreen(
                    gasto = g,
                    viewModel = gastoViewModel,
                    onGastoActualizado = { navController.popBackStack() },
                    onVolver = { navController.popBackStack() }
                )
            }
        }

        composable(Rutas.EDITAR_PERFIL) {
            EditarPerfilScreen(
                authViewModel = authViewModel,
                onVolver = { navController.popBackStack() }
            )
        }
    }
}