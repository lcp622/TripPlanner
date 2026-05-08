/**
 * Configuración de la tipografía para la aplicación TripPlanner.
 *
 * Este archivo define los estilos de texto (fuentes, tamaños y pesos) que se
 * aplicarán de forma global siguiendo las directrices de Material Design 3.
 *
 * La tipografía ayuda a mantener la jerarquía visual y la legibilidad en
 * todas las pantallas de la aplicación.
 */
package dam.pmdm.tripplanner.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Conjunto de estilos de texto predeterminados para la interfaz de usuario.
 *
 * Se utiliza dentro del tema [TripPlannerTheme] para aplicar una apariencia
 * coherente a los componentes de texto.
 *
 */
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )

)