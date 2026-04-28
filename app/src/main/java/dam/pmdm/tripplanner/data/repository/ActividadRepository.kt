package dam.pmdm.tripplanner.data.repository

import dam.pmdm.tripplanner.data.local.dao.ActividadDao
import dam.pmdm.tripplanner.data.local.entity.ActividadEntity
import kotlinx.coroutines.flow.Flow

class ActividadRepository(private val actividadDao: ActividadDao) {

    fun obtenerActividades(idViaje: String): Flow<List<ActividadEntity>> {
        return actividadDao.obtenerActividadesPorViaje(idViaje)
    }

    suspend fun crearActividad(actividad: ActividadEntity) {
        actividadDao.insertar(actividad)
    }

    suspend fun actualizarActividad(actividad: ActividadEntity) {
        actividadDao.actualizar(actividad)
    }

    suspend fun eliminarActividad(actividad: ActividadEntity) {
        actividadDao.eliminar(actividad)
    }
}