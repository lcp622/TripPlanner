package dam.pmdm.tripplanner.ui

import android.app.Application
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dam.pmdm.tripplanner.data.local.TripPlannerDatabase
import dam.pmdm.tripplanner.data.repository.AuthRepository
import dam.pmdm.tripplanner.data.repository.ViajeRepository
import dam.pmdm.tripplanner.ui.auth.AuthViewModel
import dam.pmdm.tripplanner.ui.auth.LoginScreen
import dam.pmdm.tripplanner.ui.auth.RegisterScreen
import dam.pmdm.tripplanner.ui.viajes.ViajeViewModel
import dam.pmdm.tripplanner.ui.viajes.ViajeViewModelFactory
import dam.pmdm.tripplanner.ui.viajes.ViajesScreen
import androidx.compose.ui.platform.LocalContext

object Rutas {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val VIAJES = "viajes"
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
                onNuevoViaje = { /* próximamente */ },
                onViajeClick = { /* próximamente */ }
            )
        }
    }
}