package dam.pmdm.tripplanner.data.local.dao

import androidx.room.*
import dam.pmdm.tripplanner.data.local.entity.ActividadEntity

@Dao
interface ActividadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(actividad: ActividadEntity)

    @Update
    suspend fun actualizar(actividad: ActividadEntity)

    @Delete
    suspend fun eliminar(actividad: ActividadEntity)

    @Query("SELECT * FROM ACTIVIDAD WHERE idActividad = :idActividad")
    suspend fun obtenerPorId(idActividad: String): ActividadEntity?
    @Query("SELECT * FROM ACTIVIDAD")
    suspend fun obtenerTodasLasActividades(): List<ActividadEntity>
}