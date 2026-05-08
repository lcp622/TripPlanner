package dam.pmdm.tripplanner.ui.gastos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dam.pmdm.tripplanner.data.local.entity.GastoEntity
import dam.pmdm.tripplanner.data.repository.GastoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Representa los diferentes estados de la interfaz de usuario para la gestión de gastos.
 */
sealed class GastoUiState {
    /** Estado de carga inicial. */
    object Loading : GastoUiState()

    /**
     * Estado de éxito cuando los datos se han cargado correctamente.
     * @property gastos Lista de entidades de gastos obtenidas.
     */
    data class Success(val gastos: List<GastoEntity>) : GastoUiState()

    /**
     * Estado de error cuando ocurre un fallo en la obtención de datos.
     * @property mensaje Descripción del error ocurrido.
     */
    data class Error(val mensaje: String) : GastoUiState()
}

/**
 * ViewModel encargado de la lógica de negocio relacionada con los gastos de un viaje.
 *
 * Gestiona la carga, creación, actualización y eliminación de gastos, así como el
 * reparto de los mismos entre los participantes.
 *
 * @property repository Repositorio de datos para acceder a la fuente de datos de gastos.
 */
class GastoViewModel(private val repository: GastoRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<GastoUiState>(GastoUiState.Loading)

    /**
     * Flujo de estado que observa la UI para reaccionar a cambios en los gastos.
     */
    val uiState: StateFlow<GastoUiState> = _uiState

    /**
     * Carga la lista de gastos asociados a un viaje específico.
     *
     * @param idViaje Identificador único del viaje.
     */
    fun cargarGastos(idViaje: String) {
        viewModelScope.launch {
            repository.obtenerGastos(idViaje)
                .catch { e -> _uiState.value = GastoUiState.Error(e.message ?: "Error desconocido") }
                .collect { gastos -> _uiState.value = GastoUiState.Success(gastos) }
        }
    }

    /**
     * Crea un nuevo gasto y registra su reparto entre los participantes.
     *
     * @param idViaje ID del viaje al que pertenece el gasto.
     * @param idPagador ID del usuario que realizó el pago.
     * @param nombrePagador Nombre del usuario que pagó.
     * @param concepto Descripción o motivo del gasto.
     * @param importe Cantidad total del gasto.
     * @param categoria Categoría del gasto (comida, transporte, etc.).
     * @param notas Comentarios adicionales (opcional).
     * @param participantes Lista de mapas que contienen la información de los usuarios que comparten el gasto.
     */
    fun crearGastoConReparto(
        idViaje: String,
        idPagador: String,
        nombrePagador: String,
        concepto: String,
        importe: Double,
        categoria: String,
        notas: String?,
        participantes: List<Map<String, Any>>
    ) {
        viewModelScope.launch {
            val gasto = GastoEntity(
                idGasto = UUID.randomUUID().toString(),
                idViaje = idViaje,
                idPagador = idPagador,
                nombrePagador = nombrePagador,
                concepto = concepto,
                importe = importe,
                categoria = categoria,
                fecha = System.currentTimeMillis(),
                notas = notas
            )
            repository.crearGasto(gasto)
            if (participantes.isNotEmpty()) {
                repository.crearRepartoGasto(idViaje, gasto, participantes)
            }
        }
    }

    /**
     * Actualiza la información de un gasto existente en la base de datos.
     *
     * @param gasto Entidad del gasto con los datos actualizados.
     */
    fun actualizarGasto(gasto: GastoEntity) {
        viewModelScope.launch {
            repository.actualizarGasto(gasto)
        }
    }

    /**
     * Elimina un gasto de forma permanente.
     *
     * @param gasto Entidad del gasto que se desea borrar.
     */
    fun eliminarGasto(gasto: GastoEntity) {
        viewModelScope.launch {
            repository.eliminarGasto(gasto)
        }
    }

    /**
     * Calcula la suma total de una lista de gastos.
     *
     * @param gastos Lista de gastos a sumar.
     * @return El importe total acumulado.
     */
    fun totalGastos(gastos: List<GastoEntity>): Double {
        return gastos.sumOf { it.importe }
    }

    /**
     * Marca un gasto específico como saldado (pagado) para un usuario concreto.
     *
     * @param idViaje ID del viaje.
     * @param idGasto ID del gasto a saldar.
     * @param idUsuario ID del usuario que salda su deuda.
     */
    fun marcarComoSaldado(idViaje: String, idGasto: String, idUsuario: String) {
        viewModelScope.launch {
            repository.marcarComoSaldado(idViaje, idGasto, idUsuario)
        }
    }
}

/**
 * Factory para instanciar el [GastoViewModel] con las dependencias necesarias.
 *
 * @property repository El repositorio que se inyectará en el ViewModel.
 */
class GastoViewModelFactory(private val repository: GastoRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GastoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GastoViewModel(repository) as T
        }
        throw IllegalArgumentException("ViewModel desconocido")
    }
}
