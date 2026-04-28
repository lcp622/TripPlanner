package dam.pmdm.tripplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dam.pmdm.tripplanner.ui.NavGraph
import dam.pmdm.tripplanner.ui.theme.TripPlannerTheme
import androidx.compose.foundation.isSystemInDarkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TripPlannerTheme(darkTheme = isSystemInDarkTheme()) {
                NavGraph()
            }
        }
    }
}