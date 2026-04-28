package dam.pmdm.tripplanner.data.local.dao

import androidx.room.*
import dam.pmdm.tripplanner.data.local.entity.RutaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RutaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(ruta: RutaEntity)

    @Update
    suspend fun actualizar(ruta: RutaEntity)

    @Delete
    suspend fun eliminar(ruta: RutaEntity)

    @Query("SELECT * FROM RUTA WHERE idViaje = :idViaje ORDER BY fechaCreacion DESC")
    fun obtenerRutasPorViaje(idViaje: String): Flow<List<RutaEntity>>

    @Query("SELECT * FROM RUTA WHERE idRuta = :idRuta")
    suspend fun obtenerPorId(idRuta: String): RutaEntity?

    @Query("DELETE FROM RUTA WHERE idViaje = :idViaje")
    suspend fun eliminarPorViaje(idViaje: String)
}