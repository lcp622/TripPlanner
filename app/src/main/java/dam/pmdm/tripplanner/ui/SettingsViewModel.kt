package dam.pmdm.tripplanner.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dam.pmdm.tripplanner.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel que gestiona las preferencias de configuración de la aplicación.
 * Actualmente gestiona el modo oscuro, pero puede ampliarse con más preferencias.
 *
 *
 * @param application Aplicación Android para acceder al contexto
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    /** Repositorio que gestiona la persistencia de preferencias con DataStore */
    private val repository = SettingsRepository(application.applicationContext)

    /**
     * Estado observable del modo oscuro expuesto a la UI.
     * Se convierte el Flow de DataStore a StateFlow para que Compose pueda
     * observarlo con [androidx.compose.runtime.collectAsState].
     *
     * Se usa [SharingStarted.WhileSubscribed] con un timeout de 5 segundos
     * para mantener el Flow activo durante rotaciones de pantalla sin
     * relanzarlo innecesariamente — optimización recomendada por Google.
     *
     * El valor inicial es false (modo claro) hasta que DataStore emite el
     * valor almacenado en disco.
     */
    val isDarkMode: StateFlow<Boolean> = repository.isDarkMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    /**
     * Activa o desactiva el modo oscuro y persiste la preferencia en DataStore.
     * La operación es asíncrona y se ejecuta en el scope del ViewModel para
     * que se cancele automáticamente si el ViewModel se destruye.
     *
     * @param enabled true para activar el modo oscuro, false para desactivarlo
     */
    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDarkMode(enabled)
        }
    }
}