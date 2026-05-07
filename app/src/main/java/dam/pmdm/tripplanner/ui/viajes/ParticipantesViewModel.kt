package dam.pmdm.tripplanner.ui.viajes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dam.pmdm.tripplanner.data.repository.FirestoreViajeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ParticipanteUiState {
    object Idle : ParticipanteUiState()
    object Loading : ParticipanteUiState()
    object Success : ParticipanteUiState()
    data class Error(val mensaje: String) : ParticipanteUiState()
}

class ParticipantesViewModel(private val repository: FirestoreViajeRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<ParticipanteUiState>(ParticipanteUiState.Idle)
    val uiState: StateFlow<ParticipanteUiState> = _uiState

    fun añadirParticipante(idViaje: String, email: String) {
        viewModelScope.launch {
            _uiState.value = ParticipanteUiState.Loading
            val resultado = repository.anadirParticipante(idViaje, email)
            _uiState.value = if (resultado.isSuccess) {
                ParticipanteUiState.Success
            } else {
                ParticipanteUiState.Error(resultado.exceptionOrNull()?.message ?: "Error desconocido")
            }
        }
    }

    fun resetState() {
        _uiState.value = ParticipanteUiState.Idle
    }

    fun eliminarParticipante(idViaje: String, idUsuario: String) {
        viewModelScope.launch {
            repository.eliminarParticipante(idViaje, idUsuario)
        }
    }
}

class ParticipantesViewModelFactory(
    private val repository: FirestoreViajeRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ParticipantesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ParticipantesViewModel(repository) as T
        }
        throw IllegalArgumentException("ViewModel desconocido")
    }
}