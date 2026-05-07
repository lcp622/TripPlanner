package dam.pmdm.tripplanner

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.Coil
import coil.ImageLoader
import dam.pmdm.tripplanner.ui.NavGraph
import dam.pmdm.tripplanner.ui.SettingsViewModel
import dam.pmdm.tripplanner.ui.theme.TripPlannerTheme
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        }

        val imageLoader = ImageLoader.Builder(this)
            .crossfade(true)
            .build()
        Coil.setImageLoader(imageLoader)

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
            TripPlannerTheme(darkTheme = isDarkMode) {
                NavGraph(settingsViewModel = settingsViewModel)
            }
        }
    }
}