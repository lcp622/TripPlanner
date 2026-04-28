package dam.pmdm.tripplanner.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import dam.pmdm.tripplanner.data.local.TripPlannerDatabase
import dam.pmdm.tripplanner.data.local.entity.UsuarioEntity
import dam.pmdm.tripplanner.data.repository.ViajeRepository
import dam.pmdm.tripplanner.ui.auth.AuthViewModel
import dam.pmdm.tripplanner.ui.auth.LoginScreen
import dam.pmdm.tripplanner.ui.auth.RegisterScreen
import dam.pmdm.tripplanner.ui.viajes.CrearViajeScreen
import dam.pmdm.tripplanner.ui.viajes.ViajeViewModel
import dam.pmdm.tripplanner.ui.viajes.ViajeViewModelFactory
import dam.pmdm.tripplanner.ui.viajes.ViajesScreen
import kotlinx.coroutines.launch

object Rutas {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val VIAJES = "viajes"
    const val CREAR_VIAJE = "crear_viaje"
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val db = TripPlannerDatabase.getInstance(context)
    val viajeRepository = ViajeRepository(db.viajeDao())
    val viajeViewModel: ViajeViewModel = viewModel(factory = ViajeViewModelFactory(viajeRepository))
    val scope = rememberCoroutineScope()

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

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Rutas.LOGIN) {
            LoginScreen(
                onLoginExitoso = {
                    navController.navigate(Rutas.VIAJES) {
                        popUpTo(Rutas.LOGIN) { inclusive = true }
                    }
                },
                onIrARegistro = {
                    navController.navigate(Rutas.REGISTER)
                }
            )
        }

        composable(Rutas.REGISTER) {
            RegisterScreen(
                onRegistroExitoso = {
                    navController.navigate(Rutas.VIAJES) {
                        popUpTo(Rutas.LOGIN) { inclusive = true }
                    }
                },
                onVolverALogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Rutas.VIAJES) {
            ViajesScreen(
                viewModel = viajeViewModel,
                onNuevoViaje = {
                    navController.navigate(Rutas.CREAR_VIAJE)
                },
                onViajeClick = { /* próximamente */ }
            )
        }

        composable(Rutas.CREAR_VIAJE) {
            CrearViajeScreen(
                viewModel = viajeViewModel,
                onViajeCreado = {
                    navController.popBackStack()
                },
                onVolver = {
                    navController.popBackStack()
                }
            )
        }
    }
}