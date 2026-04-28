package dam.pmdm.tripplanner.data.repository

import com.google.firebase.auth.FirebaseAuth
import dam.pmdm.tripplanner.data.local.dao.ViajeDao
import dam.pmdm.tripplanner.data.local.entity.ViajeEntity
import kotlinx.coroutines.flow.Flow

class ViajeRepository(private val viajeDao: ViajeDao) {

    private val auth = FirebaseAuth.getInstance()

    private val idUsuarioActual: String
        get() = auth.currentUser?.uid ?: ""

    fun obtenerMisViajes(): Flow<List<ViajeEntity>> {
        return viajeDao.obtenerViajesPorUsuario(idUsuarioActual)
    }

    fun obtenerViajesPorEstado(estado: String): Flow<List<ViajeEntity>> {
        return viajeDao.obtenerPorEstado(estado)
    }

    suspend fun obtenerViajePorId(idViaje: String): ViajeEntity? {
        return viajeDao.obtenerPorId(idViaje)
    }


    suspend fun crearViaje(viaje: ViajeEntity) {
        viajeDao.insertar(viaje)
    }

    suspend fun actualizarViaje(viaje: ViajeEntity) {
        viajeDao.actualizar(viaje)
    }

    suspend fun eliminarViaje(idViaje: String) {
        viajeDao.eliminarPorId(idViaje)
    }

    fun obtenerTodos(): Flow<List<ViajeEntity>> {
        return viajeDao.obtenerTodos()
    }
}