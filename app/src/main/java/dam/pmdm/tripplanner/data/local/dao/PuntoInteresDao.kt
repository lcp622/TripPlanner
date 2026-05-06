package dam.pmdm.tripplanner.data.local.dao

import androidx.room.*
import dam.pmdm.tripplanner.data.local.entity.PuntoInteresEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PuntoInteresDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(punto: PuntoInteresEntity)

    @Delete
    suspend fun eliminar(punto: PuntoInteresEntity)

    @Query("SELECT * FROM PUNTO_INTERES WHERE idViaje = :idViaje ORDER BY orden ASC")
    fun obtenerPuntosPorViaje(idViaje: String): Flow<List<PuntoInteresEntity>>

    @Query("DELETE FROM PUNTO_INTERES WHERE idViaje = :idViaje")
    suspend fun eliminarPorViaje(idViaje: String)

    @Query("DELETE FROM PUNTO_INTERES WHERE idPunto = :idPunto")
    suspend fun eliminarPorId(idPunto: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(puntos: List<PuntoInteresEntity>)
}