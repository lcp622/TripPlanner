package dam.pmdm.tripplanner.ui.viajes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dam.pmdm.tripplanner.data.repository.ViajeRepository

class ViajeViewModelFactory(private val repository: ViajeRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ViajeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ViajeViewModel(repository) as T
        }
        throw IllegalArgumentException("ViewModel desconocido")
    }
}