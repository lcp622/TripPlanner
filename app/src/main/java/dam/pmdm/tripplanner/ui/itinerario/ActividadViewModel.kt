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

/**
 * Representa los diferentes estados de la interfaz de usuario para la lista de actividades.
 */
sealed class ActividadUiState {
    /** Estado de carga mientras se obtienen los datos. */
    object Loading : ActividadUiState()

    /**
     * Estado de éxito tras cargar las actividades.
     * @property actividades Lista de actividades ordenadas por fecha y hora.
     */
    data class Success(val actividades: List<ActividadEntity>) : ActividadUiState()

    /**
     * Estado de error en caso de fallo en la obtención de datos.
     * @property mensaje Descripción del error.
     */
    data class Error(val mensaje: String) : ActividadUiState()
}

/**
 * ViewModel encargado de gestionar el itinerario (actividades) de un viaje.
 *
 * Se encarga de la comunicación entre la UI y el repositorio de actividades,
 * manejando operaciones CRUD y el estado de la pantalla.
 *
 * @property repository El repositorio de datos de actividades.
 */
class ActividadViewModel(private val repository: ActividadRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<ActividadUiState>(ActividadUiState.Loading)

    /**
     * Flujo de estado expuesto a la UI para observar los cambios en las actividades.
     */
    val uiState: StateFlow<ActividadUiState> = _uiState

    /**
     * Carga las actividades de un viaje específico desde el repositorio.
     *
     * Las actividades se devuelven ordenadas cronológicamente por fecha y hora de inicio.
     *
     * @param idViaje Identificador único del viaje.
     */
    fun cargarActividades(idViaje: String) {
        viewModelScope.launch {
            repository.obtenerActividades(idViaje)
                .catch { e -> _uiState.value = ActividadUiState.Error(e.message ?: "Error desconocido") }
                .collect { actividades ->
                    _uiState.value = ActividadUiState.Success(
                        actividades.sortedWith(compareBy({ it.fecha }, { it.horaInicio }))
                    )
                }
        }
    }

    /**
     * Crea y guarda una nueva actividad en el itinerario.
     *
     * @param idViaje ID del viaje al que pertenece la actividad.
     * @param titulo Nombre o título de la actividad.
     * @param descripcion Detalle adicional de la actividad (opcional).
     * @param fecha Fecha de la actividad en milisegundos.
     * @param horaInicio Hora de comienzo en formato String (opcional).
     * @param horaFin Hora de finalización en formato String (opcional).
     * @param lugar Ubicación o nombre del sitio donde se realiza (opcional).
     */
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

    /**
     * Elimina una actividad existente de la base de datos.
     *
     * @param actividad La entidad de la actividad a eliminar.
     */
    fun eliminarActividad(actividad: ActividadEntity) {
        viewModelScope.launch {
            repository.eliminarActividad(actividad)
        }
    }

    /**
     * Actualiza los datos de una actividad existente.
     *
     * @param actividad La entidad con los datos modificados.
     */
    fun actualizarActividad(actividad: ActividadEntity) {
        viewModelScope.launch {
            repository.actualizarActividad(actividad)
        }
    }
}

/**
 * Clase Factory para instanciar el [ActividadViewModel] con su dependencia de repositorio.
 *
 * @property repository Repositorio que será inyectado en el ViewModel.
 */
class ActividadViewModelFactory(private val repository: ActividadRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ActividadViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ActividadViewModel(repository) as T
        }
        throw IllegalArgumentException("ViewModel desconocido")
    }
}