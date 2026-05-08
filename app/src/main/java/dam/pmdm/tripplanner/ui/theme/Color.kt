package dam.pmdm.tripplanner.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Definición de la paleta de colores oficial de la aplicación TripPlanner.
 *
 * Este archivo centraliza todos los valores cromáticos utilizados en los temas
 * claro y oscuro, asegurando la consistencia visual en toda la interfaz.
 */

// --- Colores Primarios ---

/** Color azul principal utilizado en botones, barras de navegación y elementos destacados. */
val TripBlue = Color(0xFF2196F3)

/** Color verde azulado para elementos secundarios y acentos visuales refrescantes. */
val TripTeal = Color(0xFF00BCD4)

/** Versión oscura del azul principal para estados presionados o sombras. */
val TripBlueDark = Color(0xFF1565C0)


// --- Colores de Acento ---

/** Color naranja vibrante para llamadas a la acción (CTA) y alertas visuales. */
val TripOrange = Color(0xFFFF7043)


// --- Colores Neutros ---

/** Color blanco puro para superficies y fondos claros. */
val TripWhite = Color(0xFFFFFFFF)

/** Color de fondo gris muy tenue para separar secciones en la interfaz. */
val TripBackground = Color(0xFFF5F7FA)

/** Color para superficies elevadas como tarjetas (Cards). */
val TripSurface = Color(0xFFFFFFFF)

/** Color gris estándar para bordes y divisores. */
val TripGray = Color(0xFF9E9E9E)

/** Color gris muy claro para fondos de campos de texto o deshabilitados. */
val TripGrayLight = Color(0xFFEEEEEE)

/** Color de texto principal (casi negro) para máxima legibilidad. */
val TripTextPrimary = Color(0xFF212121)

/** Color de texto secundario (gris) para información menos relevante o etiquetas. */
val TripTextSecondary = Color(0xFF757575)


// --- Colores de Estado de Viaje ---

/** Color representativo para viajes en estado 'Planificado'. */
val ColorPlanificado = Color(0xFF2196F3)  // Azul

/** Color representativo para viajes que se encuentran actualmente 'En curso'. */
val ColorEnCurso = Color(0xFFFF9800)      // Naranja/Amarillo

/** Color representativo para viajes que ya han 'Finalizado'. */
val ColorFinalizado = Color(0xFFE53935)   // Rojo