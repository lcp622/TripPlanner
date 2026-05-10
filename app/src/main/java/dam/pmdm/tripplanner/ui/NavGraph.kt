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

/**
 * Objeto que centraliza todas las rutas de navegación de la aplicación.
 * Se usa como fuente única de verdad para los identificadores de ruta,
 * evitando el uso de strings literales dispersos por el código.
 */
object Rutas {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAIN = "main"
    /** Ruta del onboarding — solo se muestra en la primera ejecución */
    const val ONBOARDING = "onboarding"
    const val CREAR_VIAJE = "crear_viaje"
    const val DETALLE_VIAJE = "detalle_viaje/{idViaje}"
    const val EDITAR_VIAJE = "editar_viaje/{idViaje}"
    const val CREAR_ACTIVIDAD = "crear_actividad/{idViaje}"
    const val CREAR_GASTO = "crear_gasto/{idViaje}"
    const val EDITAR_ACTIVIDAD = "editar_actividad/{idViaje}/{idActividad}"
    const val EDITAR_GASTO = "editar_gasto/{idViaje}/{idGasto}"
    const val EDITAR_PERFIL = "editar_perfil"
}

/**
 * Grafo de navegación principal de TripPlanner.
 * Define todas las pantallas de la app y las transiciones entre ellas.
 *
 * Se desactivan las animaciones de transición con [EnterTransition.None] y
 * [ExitTransition.None] para un comportamiento más fluido en la navegación.
 *
 * Los ViewModels y repositorios se crean aquí y se comparten entre pantallas
 * para evitar recrearlos en cada navegación y mantener el estado de la UI.
 *
 * @param navController Controlador de navegación de Compose
 * @param authViewModel ViewModel de autenticación
 * @param settingsViewModel ViewModel de ajustes para el onboarding y modo oscuro
 * @param startDestination Ruta inicial calculada en MainActivity según el estado del usuario
 */
@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel(),
    settingsViewModel: SettingsViewModel,
    startDestination: String
) {
    val context = LocalContext.current
    val db = TripPlannerDatabase.getInstance(context)
    val scope = rememberCoroutineScope()

    // Repositorios y ViewModels compartidos entre pantallas
    val viajeRepository = FirestoreViajeRepository(db.viajeDao())
    val viajeViewModel: ViajeViewModel = viewModel(factory = ViajeViewModelFactory(viajeRepository))

    val actividadRepository = ActividadRepository(db.actividadDao())
    val actividadViewModel: ActividadViewModel = viewModel(factory = ActividadViewModelFactory(actividadRepository))

    val gastoRepository = GastoRepository(db.gastoDao())
    val gastoViewModel: GastoViewModel = viewModel(factory = GastoViewModelFactory(gastoRepository))

    // Cachear el usuario autenticado en Room al iniciar la app
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

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {

        // Pantalla de onboarding — solo se muestra en la primera ejecución
        composable(Rutas.ONBOARDING) {
            OnboardingScreen(
                onTerminar = {
                    settingsViewModel.completarOnboarding()
                    navController.navigate(Rutas.LOGIN) {
                        popUpTo(Rutas.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        // Pantalla de inicio de sesión
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

        // Pantalla de registro de nuevo usuario
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

        // Pantalla principal con navegación por pestañas
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

        // Pantalla de creación de un nuevo viaje
        composable(Rutas.CREAR_VIAJE) {
            CrearViajeScreen(
                viewModel = viajeViewModel,
                onViajeCreado = { navController.popBackStack() },
                onVolver = { navController.popBackStack() }
            )
        }

        // Pantalla de detalle de un viaje — carga el viaje desde Room o Firestore
        composable(Rutas.DETALLE_VIAJE) { backStackEntry ->
            val idViaje = backStackEntry.arguments?.getString("idViaje") ?: ""
            var viaje by remember(idViaje) { mutableStateOf<ViajeEntity?>(null) }

            LaunchedEffect(idViaje) {
                viaje = viajeRepository.obtenerViajePorIdFirestore(idViaje)
            }

            // Mostrar indicador de carga mientras se obtiene el viaje
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

        // Pantalla de edición de actividad — carga la actividad desde Room
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

        // Pantalla de edición de viaje — carga el viaje desde Room o Firestore
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

        // Pantalla de creación de actividad para un viaje
        composable(Rutas.CREAR_ACTIVIDAD) { backStackEntry ->
            val idViaje = backStackEntry.arguments?.getString("idViaje") ?: ""
            CrearActividadScreen(
                idViaje = idViaje,
                viewModel = actividadViewModel,
                onActividadCreada = { navController.popBackStack() },
                onVolver = { navController.popBackStack() }
            )
        }

        // Pantalla de creación de gasto para un viaje
        composable(Rutas.CREAR_GASTO) { backStackEntry ->
            val idViaje = backStackEntry.arguments?.getString("idViaje") ?: ""
            CrearGastoScreen(
                idViaje = idViaje,
                viewModel = gastoViewModel,
                onGastoCreado = { navController.popBackStack() },
                onVolver = { navController.popBackStack() }
            )
        }

        // Pantalla de edición de gasto — carga el gasto desde Room
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

        // Pantalla de edición de perfil del usuario
        composable(Rutas.EDITAR_PERFIL) {
            EditarPerfilScreen(
                authViewModel = authViewModel,
                onVolver = { navController.popBackStack() }
            )
        }
    }
}