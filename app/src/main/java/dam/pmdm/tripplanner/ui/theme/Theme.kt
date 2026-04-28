package dam.pmdm.tripplanner.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = TripBlue,
    onPrimary = TripWhite,
    primaryContainer = TripBlueDark,
    secondary = TripTeal,
    onSecondary = TripWhite,
    tertiary = TripOrange,
    background = TripBackground,
    surface = TripSurface,
    onBackground = TripTextPrimary,
    onSurface = TripTextPrimary,
    surfaceVariant = TripGrayLight,
    onSurfaceVariant = TripTextSecondary
)

private val DarkColorScheme = darkColorScheme(
    primary = TripTeal,
    onPrimary = TripTextPrimary,
    secondary = TripBlue,
    onSecondary = TripWhite,
    tertiary = TripOrange,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onBackground = TripWhite,
    onSurface = TripWhite
)

@Composable
fun TripPlannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}