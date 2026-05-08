package dam.pmdm.tripplanner.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dam.pmdm.tripplanner.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Estados posibles de la UI de autenticación.
 * Se usa una sealed class para garantizar en tiempo de compilación
 * que todos los estados están contemplados en los bloques when.
 */
sealed class AuthUiState {
    /** Estado inicial sin operación en curso */
    object Idle : AuthUiState()
    /** Operación en curso — se muestra un indicador de carga */
    object Loading : AuthUiState()
    /** Operación completada con éxito */
    object Success : AuthUiState()
    /** Error en la operación con mensaje descriptivo */
    data class Error(val mensaje: String) : AuthUiState()
}

/**
 * ViewModel que gestiona la autenticación de usuarios.
 * Expone dos StateFlow separados:
 * - [uiState] para operaciones de login y registro
 * - [perfilState] para operaciones de edición de perfil
 *
 * Se extiende [AndroidViewModel] para acceder
 * al contexto de la aplicación necesario en [AuthRepository].
 *
 * @param application Aplicación Android para acceder al contexto
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    /** Repositorio que gestiona las operaciones de autenticación */
    private val repository = AuthRepository(application.applicationContext)

    /** Estado interno mutable de autenticación */
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)

    /** Estado observable de autenticación expuesto a la UI */
    val uiState: StateFlow<AuthUiState> = _uiState

    /** Estado interno mutable de edición de perfil */
    private val _perfilState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)

    /** Estado observable de edición de perfil expuesto a la UI */
    val perfilState: StateFlow<AuthUiState> = _perfilState

    /**
     * Indica si hay un usuario autenticado actualmente.
     * @return true si hay sesión activa, false en caso contrario
     */
    val estaAutenticado: Boolean
        get() = repository.estaAutenticado

    /**
     * Inicia sesión con email y contraseña.
     * Actualiza [uiState] a Loading durante la operación
     * y a Success o Error según el resultado.
     *
     * @param email Correo electrónico del usuario
     * @param password Contraseña del usuario
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val resultado = repository.login(email, password)
            _uiState.value = if (resultado.isSuccess) {
                AuthUiState.Success
            } else {
                AuthUiState.Error(resultado.exceptionOrNull()?.message ?: "Error desconocido")
            }
        }
    }

    /**
     * Registra un nuevo usuario con email, contraseña y nombre.
     * Actualiza [uiState] a Loading durante la operación
     * y a Success o Error según el resultado.
     *
     * @param email Correo electrónico del usuario
     * @param password Contraseña del usuario
     * @param nombre Nombre visible del usuario
     */
    fun registrar(email: String, password: String, nombre: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val resultado = repository.registrar(email, password, nombre)
            _uiState.value = if (resultado.isSuccess) {
                AuthUiState.Success
            } else {
                AuthUiState.Error(resultado.exceptionOrNull()?.message ?: "Error desconocido")
            }
        }
    }

    /**
     * Actualiza el perfil del usuario autenticado.
     * Actualiza [perfilState] a Loading durante la operación
     * y a Success o Error según el resultado.
     *
     * @param nombre Nuevo nombre del usuario
     * @param fotoUrl Nueva URL de la foto de perfil (null para no cambiarla)
     */
    fun actualizarPerfil(nombre: String, fotoUrl: String?) {
        viewModelScope.launch {
            _perfilState.value = AuthUiState.Loading
            val resultado = repository.actualizarPerfil(nombre, fotoUrl)
            _perfilState.value = if (resultado.isSuccess) {
                AuthUiState.Success
            } else {
                AuthUiState.Error(resultado.exceptionOrNull()?.message ?: "Error desconocido")
            }
        }
    }

    /**
     * Resetea el estado de edición de perfil a Idle.
     * Se llama tras procesar el resultado de [actualizarPerfil].
     */
    fun resetPerfilState() {
        _perfilState.value = AuthUiState.Idle
    }

    /**
     * Cierra la sesión del usuario y resetea el estado de autenticación.
     */
    fun cerrarSesion() {
        repository.cerrarSesion()
        _uiState.value = AuthUiState.Idle
    }

    /**
     * Resetea el estado de autenticación a Idle.
     * Se llama tras procesar el resultado de [login] o [registrar].
     */
    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}