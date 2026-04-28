package dam.pmdm.tripplanner.ui.auth

import androidx.lifecycle.ViewModel
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

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

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

    fun registrar(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val resultado = repository.registrar(email, password)
            _uiState.value = if (resultado.isSuccess) {
                AuthUiState.Success
            } else {
                AuthUiState.Error(resultado.exceptionOrNull()?.message ?: "Error desconocido")
            }
        }
    }

    fun cerrarSesion() {
        repository.cerrarSesion()
        _uiState.value = AuthUiState.Idle
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}