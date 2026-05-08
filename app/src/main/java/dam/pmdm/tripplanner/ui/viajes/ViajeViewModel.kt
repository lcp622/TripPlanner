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

/**
 * Estados posibles de la UI de viajes.
 * Se usa una sealed class para garantizar en tiempo de compilación
 * que todos los estados están contemplados en los bloques when de la UI.
 */
sealed class ViajeUiState {
    /** Cargando datos desde Firestore — se muestra un indicador circular */
    object Loading : ViajeUiState()
    /** Datos cargados correctamente con la lista de viajes */
    data class Success(val viajes: List<ViajeEntity>) : ViajeUiState()
    /** Error al cargar los datos con mensaje descriptivo */
    data class Error(val mensaje: String) : ViajeUiState()
}

/**
 * ViewModel que gestiona la lista de viajes del usuario.
 * Escucha en tiempo real los viajes desde Firestore a través de
 * [FirestoreViajeRepository.sincronizarViajes] y expone el estado
 * de la UI como [StateFlow] para que Compose pueda observarlo.
 *
 * Se puede forzar una recarga con [recargarViajes] al volver a la pantalla
 * principal desde el detalle de un viaje.
 *
 * @param repository Repositorio que gestiona la sincronización con Firestore y Room
 */
class ViajeViewModel(private val repository: FirestoreViajeRepository) : ViewModel() {

    /** Estado interno mutable de la lista de viajes */
    private val _uiState = MutableStateFlow<ViajeUiState>(ViajeUiState.Loading)

    /** Estado observable de la lista de viajes expuesto a la UI */
    val uiState: StateFlow<ViajeUiState> = _uiState

    /** Inicia la sincronización de viajes al crear el ViewModel */
    init {
        cargarViajes()
    }

    /**
     * Escucha en tiempo real los viajes del usuario desde Firestore.
     * Combina viajes propios y compartidos y actualiza el estado de la UI
     * cada vez que Firestore emite un cambio.
     *
     * Los errores del Flow se capturan con [catch] para evitar que el Flow
     * se cancele y se muestran como [ViajeUiState.Error] en la UI.
     */
    private fun cargarViajes() {
        viewModelScope.launch {
            repository.sincronizarViajes()
                .catch { e -> _uiState.value = ViajeUiState.Error(e.message ?: "Error") }
                .collect { viajes -> _uiState.value = ViajeUiState.Success(viajes) }
        }
    }

    /**
     * Fuerza una recarga de los viajes reiniciando el estado a Loading
     * y relanzando la sincronización con Firestore.
     * Se llama al volver a [dam.pmdm.tripplanner.ui.MainScreen] desde
     * el detalle de un viaje para reflejar posibles cambios.
     */
    fun recargarViajes() {
        _uiState.value = ViajeUiState.Loading
        cargarViajes()
    }

    /**
     * Crea un nuevo viaje con los datos proporcionados.
     * Genera un UUID único para el viaje y lo guarda en Firestore y Room
     * a través del repositorio. El estado inicial siempre es "PLANIFICADO".
     *
     * @param nombre Nombre descriptivo del viaje
     * @param paisDestino País o destino del viaje
     * @param fechaInicio Fecha de inicio en milisegundos (epoch)
     * @param fechaFin Fecha de fin en milisegundos (epoch)
     * @param descripcion Descripción opcional del viaje
     * @param presupuesto Presupuesto total del viaje en euros
     * @param idPropietario UID de Firebase del usuario que crea el viaje
     */
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

    /**
     * Actualiza los datos de un viaje existente en Firestore y Room.
     *
     * @param viaje Entidad del viaje con los datos actualizados
     */
    fun actualizarViaje(viaje: ViajeEntity) {
        viewModelScope.launch {
            repository.actualizarViaje(viaje)
        }
    }

    /**
     * Elimina un viaje y todas sus subcolecciones de Firestore y Room.
     *
     * @param idViaje Identificador único del viaje a eliminar
     */
    fun eliminarViaje(idViaje: String) {
        viewModelScope.launch {
            repository.eliminarViaje(idViaje)
        }
    }
}

/**
 * Factory para crear instancias de [ViajeViewModel] con su repositorio.
 * Es necesaria porque [ViajeViewModel] tiene un constructor con parámetros
 * y no puede ser instanciado directamente por el sistema de ViewModels de Android.
 *
 * @param repository Repositorio de viajes a inyectar en el ViewModel
 */
class ViajeViewModelFactory(private val repository: FirestoreViajeRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ViajeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ViajeViewModel(repository) as T
        }
        throw IllegalArgumentException("ViewModel desconocido")
    }
}