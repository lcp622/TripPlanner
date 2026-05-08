/**
 * Configuración del tema visual de la aplicación TripPlanner.
 *
 * Este archivo define los esquemas de colores para los modos claro y oscuro,
 * la tipografía y los estilos globales de los componentes de Material Design 3.
 */
package dam.pmdm.tripplanner.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.OutlinedTextFieldDefaults

/**
 * Esquema de colores para el Modo Claro.
 * Utiliza una paleta basada en [TripBlue] y fondos limpios.
 */
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

/**
 * Esquema de colores para el Modo Oscuro.
 * Optimiza el contraste para entornos con poca luz, utilizando tonos grises profundos y [TripTeal].
 */
private val DarkColorScheme = darkColorScheme(
    primary = TripTeal,
    onPrimary = Color(0xFF003747),
    primaryContainer = TripBlue,
    onPrimaryContainer = Color.White,
    secondary = TripBlue,
    onSecondary = Color.White,
    tertiary = TripOrange,
    background = Color(0xFF121212),
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFCCCCCC),
    error = Color(0xFFCF6679)
)

/**
 * Componente principal del Tema de la aplicación.
 *
 * Envuelve el contenido de la aplicación y aplica:
 * 1. El esquema de colores correspondiente (Claro u Oscuro).
 * 2. La configuración de la barra de estado del sistema (StatusBar).
 * 3. La tipografía global.
 *
 * @param darkTheme Indica si se debe aplicar el tema oscuro. Por defecto usa la configuración del sistema.
 * @param content El contenido Composable al que se aplicará el tema.
 */
@Composable
fun TripPlannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    // Configuración de la apariencia de las barras del sistema (StatusBar e Insets)
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)

            // Si el tema es claro, los iconos de la barra de estado deben ser oscuros y viceversa
            insetsController.isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Adapta automáticamente los colores del borde, etiquetas, texto y cursor basándose
 * en el estado enfocado/desenfocado y el esquema de colores activo del tema.
 *
 * @return [OutlinedTextFieldDefaults.colors] configurado con la paleta de TripPlanner.
 */
@Composable
fun tripTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.primary
)
