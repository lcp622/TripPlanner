package dam.pmdm.tripplanner.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dam.pmdm.tripplanner.ui.auth.AuthViewModel
import dam.pmdm.tripplanner.ui.auth.LoginScreen
import dam.pmdm.tripplanner.ui.auth.RegisterScreen

object Rutas {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val VIAJES = "viajes"
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
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
            // Placeholder hasta que creemos la pantalla de viajes
            androidx.compose.material3.Text("Pantalla de viajes — próximamente")
        }
    }
}