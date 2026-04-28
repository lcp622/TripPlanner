package dam.pmdm.tripplanner.ui.itinerario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dam.pmdm.tripplanner.data.local.entity.ActividadEntity
import dam.pmdm.tripplanner.data.repository.ActividadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.UUID

sealed class ActividadUiState {
    object Loading : ActividadUiState()
    data class Success(val actividades: List<ActividadEntity>) : ActividadUiState()
    data class Error(val mensaje: String) : ActividadUiState()
}

class ActividadViewModel(private val repository: ActividadRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<ActividadUiState>(ActividadUiState.Loading)
    val uiState: StateFlow<ActividadUiState> = _uiState

    fun cargarActividades(idViaje: String) {
        viewModelScope.launch {
            repository.obtenerActividades(idViaje)
                .catch { e -> _uiState.value = ActividadUiState.Error(e.message ?: "Error") }
                .collect { actividades -> _uiState.value = ActividadUiState.Success(actividades) }
        }
    }

    fun crearActividad(
        idViaje: String,
        titulo: String,
        descripcion: String?,
        fecha: Long,
        horaInicio: String?,
        horaFin: String?,
        lugar: String?
    ) {
        viewModelScope.launch {
            val actividad = ActividadEntity(
                idActividad = UUID.randomUUID().toString(),
                idViaje = idViaje,
                titulo = titulo,
                descripcion = descripcion,
                fecha = fecha,
                horaInicio = horaInicio,
                horaFin = horaFin,
                lugar = lugar
            )
            repository.crearActividad(actividad)
        }
    }

    fun eliminarActividad(actividad: ActividadEntity) {
        viewModelScope.launch {
            repository.eliminarActividad(actividad)
        }
    }
}

class ActividadViewModelFactory(private val repository: ActividadRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ActividadViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ActividadViewModel(repository) as T
        }
        throw IllegalArgumentException("ViewModel desconocido")
    }
}