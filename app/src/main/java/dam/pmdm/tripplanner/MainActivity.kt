package dam.pmdm.tripplanner

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import coil.Coil
import coil.ImageLoader
import dam.pmdm.tripplanner.ui.NavGraph
import dam.pmdm.tripplanner.ui.SettingsViewModel
import dam.pmdm.tripplanner.ui.theme.TripPlannerTheme
import okhttp3.OkHttpClient

class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val imageLoader = ImageLoader.Builder(this)
            .crossfade(true)
            .build()
        Coil.setImageLoader(imageLoader)

        setContent {
            val isDarkMode by settingsViewModel.isDarkMode.collectAsState()
            TripPlannerTheme(darkTheme = isDarkMode) {
                NavGraph(settingsViewModel = settingsViewModel)
            }
        }
    }
}