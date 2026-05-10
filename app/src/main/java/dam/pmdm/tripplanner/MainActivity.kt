package dam.pmdm.tripplanner

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.Coil
import coil.ImageLoader
import dam.pmdm.tripplanner.ui.NavGraph
import dam.pmdm.tripplanner.ui.SettingsViewModel
import dam.pmdm.tripplanner.ui.auth.AuthViewModel
import dam.pmdm.tripplanner.ui.theme.TripPlannerTheme
import java.util.concurrent.TimeUnit

/**
 * Actividad principal y punto de entrada de TripPlanner.
 * Se encarga de inicializar los componentes globales de la app:
 * - Splash screen nativo de Android
 * - Edge to edge para pantalla completa
 * - Permisos de notificaciones en Android 13+
 * - Cargador de imágenes Coil
 * - Worker periódico de notificaciones de actividades
 *
 * Calcula el destino inicial de navegación en función de:
 * 1. Si el onboarding ya se ha completado → ir al login o main
 * 2. Si hay usuario autenticado → ir al main
 * 3. En caso contrario → ir al onboarding o login
 */
class MainActivity : ComponentActivity() {

    /** ViewModel que gestiona las preferencias de la app (modo oscuro, onboarding) */
    private val settingsViewModel: SettingsViewModel by viewModels()

    /** ViewModel que gestiona la autenticación del usuario */
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Instalar el splash screen antes de setContent para que se muestre correctamente
        installSplashScreen()
        enableEdgeToEdge()

        // Solicitar permiso de notificaciones en Android 13 (API 33) o superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        }

        // Configurar Coil como cargador de imágenes global con animación de crossfade
        val imageLoader = ImageLoader.Builder(this)
            .crossfade(true)
            .build()
        Coil.setImageLoader(imageLoader)

        // Programar el worker de notificaciones para ejecutarse una vez al día
        // Se usa KEEP para no reprogramarlo si ya existe una instancia activa
        val workRequest = PeriodicWorkRequestBuilder<NotificacionWorker>(
            1, TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "notificaciones_tripplanner",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        setContent {
            val isDarkMode by settingsViewModel.isDarkMode.collectAsState()

            /**
             * Estado del onboarding obtenido desde DataStore.
             * Se recoge dentro de setContent para que sea observable
             * y actualice la UI cuando cambie.
             */
            val onboardingCompletado by settingsViewModel.onboardingCompletado.collectAsState()

            /**
             * Destino inicial de navegación calculado una sola vez con remember
             * para evitar que los cambios de tema provoquen una renavegación.
             * El orden de evaluación es:
             * 1. Si el onboarding no se ha completado → onboarding
             * 2. Si no hay usuario autenticado → login
             * 3. Si hay usuario autenticado → main
             */
            val startDestination = remember(onboardingCompletado) {
                when {
                    !onboardingCompletado -> "onboarding"
                    !authViewModel.estaAutenticado -> "login"
                    else -> "main"
                }
            }

            TripPlannerTheme(darkTheme = isDarkMode) {
                NavGraph(
                    settingsViewModel = settingsViewModel,
                    startDestination = startDestination
                )
            }
        }
    }
}