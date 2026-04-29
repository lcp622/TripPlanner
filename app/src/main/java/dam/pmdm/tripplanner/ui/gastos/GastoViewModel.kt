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

sealed class GastoUiState {
    object Loading : GastoUiState()
    data class Success(val gastos: List<GastoEntity>) : GastoUiState()
    data class Error(val mensaje: String) : GastoUiState()
}

class GastoViewModel(private val repository: GastoRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<GastoUiState>(GastoUiState.Loading)
    val uiState: StateFlow<GastoUiState> = _uiState

    fun cargarGastos(idViaje: String) {
        viewModelScope.launch {
            repository.obtenerGastos(idViaje)
                .catch { e -> _uiState.value = GastoUiState.Error(e.message ?: "Error") }
                .collect { gastos -> _uiState.value = GastoUiState.Success(gastos) }
        }
    }

    fun crearGasto(
        idViaje: String,
        idPagador: String,
        concepto: String,
        importe: Double,
        categoria: String,
        notas: String?
    ) {
        viewModelScope.launch {
            val gasto = GastoEntity(
                idGasto = UUID.randomUUID().toString(),
                idViaje = idViaje,
                idPagador = idPagador,
                concepto = concepto,
                importe = importe,
                categoria = categoria,
                fecha = System.currentTimeMillis(),
                notas = notas
            )
            repository.crearGasto(gasto)
        }
    }

    fun actualizarGasto(gasto: GastoEntity) {
        viewModelScope.launch {
            repository.actualizarGasto(gasto)
        }
    }

    fun eliminarGasto(gasto: GastoEntity) {
        viewModelScope.launch {
            repository.eliminarGasto(gasto)
        }
    }

    fun totalGastos(gastos: List<GastoEntity>): Double {
        return gastos.sumOf { it.importe }
    }

    fun gastosPorCategoria(gastos: List<GastoEntity>): Map<String, Double> {
        return gastos.groupBy { it.categoria }
            .mapValues { (_, g) -> g.sumOf { it.importe } }
    }
}

class GastoViewModelFactory(private val repository: GastoRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GastoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GastoViewModel(repository) as T
        }
        throw IllegalArgumentException("ViewModel desconocido")
    }
}