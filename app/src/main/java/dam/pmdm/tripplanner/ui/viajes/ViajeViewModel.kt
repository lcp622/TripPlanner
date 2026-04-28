package dam.pmdm.tripplanner.ui.viajes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dam.pmdm.tripplanner.data.local.entity.ViajeEntity
import dam.pmdm.tripplanner.data.repository.ViajeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.UUID

sealed class ViajeUiState {
    object Loading : ViajeUiState()
    data class Success(val viajes: List<ViajeEntity>) : ViajeUiState()
    data class Error(val mensaje: String) : ViajeUiState()
}

class ViajeViewModel(private val repository: ViajeRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<ViajeUiState>(ViajeUiState.Loading)
    val uiState: StateFlow<ViajeUiState> = _uiState

    init {
        cargarViajes()
    }

    private fun cargarViajes() {
        val idUsuario = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
        viewModelScope.launch {
            repository.obtenerMisViajes()
                .catch { e -> _uiState.value = ViajeUiState.Error(e.message ?: "Error") }
                .collect { viajes -> _uiState.value = ViajeUiState.Success(viajes) }
        }
    }

    fun crearViaje(
        nombre: String,
        paisDestino: String,
        fechaInicio: Long,
        fechaFin: Long,
        descripcion: String?,
        presupuesto: Double,
        idPropietario: String
    ) {
        viewModelScope.launch {
            val viaje = ViajeEntity(
                idViaje = UUID.randomUUID().toString(),
                nombre = nombre,
                paisDestino = paisDestino,
                fechaInicio = fechaInicio,
                fechaFin = fechaFin,
                descripcion = descripcion,
                presupuestoTotal = presupuesto,
                estado = "PLANIFICADO",
                idPropietario = idPropietario
            )
            repository.crearViaje(viaje)
        }
    }

    fun eliminarViaje(idViaje: String) {
        viewModelScope.launch {
            repository.eliminarViaje(idViaje)
        }
    }
}