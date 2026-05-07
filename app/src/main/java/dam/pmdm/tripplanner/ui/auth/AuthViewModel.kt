package dam.pmdm.tripplanner.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dam.pmdm.tripplanner.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val mensaje: String) : AuthUiState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository(application.applicationContext)
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState
    private val _perfilState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val perfilState: StateFlow<AuthUiState> = _perfilState


    val estaAutenticado: Boolean
        get() = repository.estaAutenticado

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

    fun resetPerfilState() {
        _perfilState.value = AuthUiState.Idle
    }

    fun cerrarSesion() {
        repository.cerrarSesion()
        _uiState.value = AuthUiState.Idle
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}