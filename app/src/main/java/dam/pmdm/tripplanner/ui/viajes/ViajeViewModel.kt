package dam.pmdm.tripplanner.ui.viajes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dam.pmdm.tripplanner.data.local.entity.ViajeEntity
import dam.pmdm.tripplanner.data.repository.FirestoreViajeRepository
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

class ViajeViewModel(private val repository: FirestoreViajeRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<ViajeUiState>(ViajeUiState.Loading)
    val uiState: StateFlow<ViajeUiState> = _uiState

    init {
        cargarViajes()
    }

    private fun cargarViajes() {
        viewModelScope.launch {
            repository.sincronizarViajes()
                .catch { e -> _uiState.value = ViajeUiState.Error(e.message ?: "Error") }
                .collect { viajes -> _uiState.value = ViajeUiState.Success(viajes) }
        }
    }

    fun recargarViajes() {
        _uiState.value = ViajeUiState.Loading
        cargarViajes()
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

    fun actualizarViaje(viaje: ViajeEntity) {
        viewModelScope.launch {
            repository.actualizarViaje(viaje)
        }
    }

    fun eliminarViaje(idViaje: String) {
        viewModelScope.launch {
            repository.eliminarViaje(idViaje)
        }
    }
}
fun calcularEstado(fechaInicio: Long, fechaFin: Long): String {
    val ahora = System.currentTimeMillis()
    return when {
        ahora < fechaInicio -> "PLANIFICADO"
        ahora > fechaFin -> "FINALIZADO"
        else -> "EN_CURSO"
    }
}

class ViajeViewModelFactory(private val repository: FirestoreViajeRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ViajeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ViajeViewModel(repository) as T
        }
        throw IllegalArgumentException("ViewModel desconocido")
    }
}