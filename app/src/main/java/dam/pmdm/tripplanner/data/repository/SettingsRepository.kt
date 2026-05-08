package dam.pmdm.tripplanner.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Extensión de [Context] para acceder al DataStore de preferencias.
 * Se define como extensión para garantizar una única instancia del DataStore
 * en toda la aplicación (patrón Singleton recomendado por Google).
 * El nombre "settings" identifica el archivo de preferencias en disco.
 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Repositorio que gestiona las preferencias de usuario de la aplicación.
 * Usa Jetpack DataStore para persistir las preferencias de forma asíncrona
 * y segura, como alternativa moderna a SharedPreferences.
 *
 * Se elige DataStore sobre SharedPreferences porque soporta corrutinas
 * y Flow de Kotlin de forma nativa, evitando bloqueos en el hilo principal.
 *
 * @param context Contexto de la aplicación para acceder al DataStore
 */
class SettingsRepository(private val context: Context) {

    companion object {
        /**
         * Clave para almacenar la preferencia de modo oscuro en DataStore.
         * Se define como constante para evitar errores tipográficos.
         */
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
    }

    /**
     * Flow que emite el estado actual del modo oscuro.
     * Emite false por defecto si la preferencia no está definida.
     * Se actualiza automáticamente cada vez que cambia el valor en DataStore.
     */
    val isDarkMode: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[DARK_MODE_KEY] ?: false
        }

    /**
     * Actualiza la preferencia de modo oscuro en DataStore.
     * La operación es suspendida para ejecutarse en una corrutina
     * sin bloquear el hilo principal.
     *
     * @param enabled true para activar el modo oscuro, false para desactivarlo
     */
    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }
}