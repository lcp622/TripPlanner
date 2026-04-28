package dam.pmdm.tripplanner.data.local.dao

import androidx.room.*
import dam.pmdm.tripplanner.data.local.entity.ActividadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActividadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(actividad: ActividadEntity)

    @Update
    suspend fun actualizar(actividad: ActividadEntity)

    @Delete
    suspend fun eliminar(actividad: ActividadEntity)

    @Query("SELECT * FROM ACTIVIDAD WHERE idViaje = :idViaje ORDER BY fecha ASC, horaInicio ASC")
    fun obtenerActividadesPorViaje(idViaje: String): Flow<List<ActividadEntity>>

    @Query("SELECT * FROM ACTIVIDAD WHERE idActividad = :idActividad")
    suspend fun obtenerPorId(idActividad: String): ActividadEntity?

    @Query("DELETE FROM ACTIVIDAD WHERE idViaje = :idViaje")
    suspend fun eliminarPorViaje(idViaje: String)
}