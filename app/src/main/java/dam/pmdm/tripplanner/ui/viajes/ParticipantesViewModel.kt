package dam.pmdm.tripplanner.ui.viajes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dam.pmdm.tripplanner.data.repository.FirestoreViajeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Estados posibles de la UI de gestión de participantes.
 * Se usa una sealed class para garantizar en tiempo de compilación
 * que todos los estados están contemplados en los bloques when de la UI.
 */
sealed class ParticipanteUiState {
    /** Estado inicial sin operación en curso */
    object Idle : ParticipanteUiState()
    /** Operación en curso — se muestra un indicador de carga */
    object Loading : ParticipanteUiState()
    /** Operación completada con éxito */
    object Success : ParticipanteUiState()
    /** Error en la operación con mensaje descriptivo */
    data class Error(val mensaje: String) : ParticipanteUiState()
}

/**
 * ViewModel que gestiona las operaciones sobre los participantes de un viaje.
 * Expone el estado de la UI como [StateFlow] para que Compose pueda observarlo.
 *
 * Las operaciones de añadir y eliminar participantes se delegan al
 * [FirestoreViajeRepository] que gestiona la sincronización con Firestore.
 *
 * @param repository Repositorio que gestiona las operaciones de participantes en Firestore
 */
class ParticipantesViewModel(private val repository: FirestoreViajeRepository) : ViewModel() {

    /** Estado interno mutable de las operaciones sobre participantes */
    private val _uiState = MutableStateFlow<ParticipanteUiState>(ParticipanteUiState.Idle)

    /** Estado observable expuesto a la UI para mostrar loading, éxito o error */
    val uiState: StateFlow<ParticipanteUiState> = _uiState

    /**
     * Añade un participante al viaje buscándolo por su email en Firestore.
     * Actualiza el estado a Loading durante la operación y a Success o Error
     * según el resultado devuelto por el repositorio.
     *
     * @param idViaje Identificador del viaje al que se añade el participante
     * @param email Email del usuario a añadir como participante
     */
    fun anadirParticipante(idViaje: String, email: String) {
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

    /**
     * Resetea el estado de la UI a Idle.
     * Se llama tras procesar el resultado de [anadirParticipante]
     * para que la UI vuelva al estado inicial y no muestre mensajes residuales.
     */
    fun resetState() {
        _uiState.value = ParticipanteUiState.Idle
    }

    /**
     * Elimina un participante del viaje en Firestore.
     * No actualiza el [uiState] porque la lista de participantes se actualiza
     * automáticamente a través del listener en tiempo real de Firestore.
     *
     * @param idViaje Identificador del viaje del que se elimina el participante
     * @param idUsuario UID del usuario a eliminar como participante
     */
    fun eliminarParticipante(idViaje: String, idUsuario: String) {
        viewModelScope.launch {
            repository.eliminarParticipante(idViaje, idUsuario)
        }
    }
}

/**
 * Factory para crear instancias de [ParticipantesViewModel] con su repositorio.
 * Es necesaria porque [ParticipantesViewModel] tiene un constructor con parámetros
 * y no puede ser instanciado directamente por el sistema de ViewModels de Android.
 *
 * @param repository Repositorio de viajes a inyectar en el ViewModel
 */
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