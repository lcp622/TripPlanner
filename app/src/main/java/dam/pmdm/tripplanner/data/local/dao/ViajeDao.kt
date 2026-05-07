package dam.pmdm.tripplanner.data.local.dao

import androidx.room.*
import dam.pmdm.tripplanner.data.local.entity.ViajeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ViajeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(viaje: ViajeEntity)

    @Update
    suspend fun actualizar(viaje: ViajeEntity)

    @Query("SELECT * FROM VIAJE WHERE idPropietario = :idUsuario ORDER BY fechaInicio DESC")
    fun obtenerViajesPorUsuario(idUsuario: String): Flow<List<ViajeEntity>>

    @Query("SELECT * FROM VIAJE WHERE idViaje = :idViaje")
    suspend fun obtenerPorId(idViaje: String): ViajeEntity?

    @Query("SELECT * FROM VIAJE WHERE estado = :estado ORDER BY fechaInicio DESC")
    fun obtenerPorEstado(estado: String): Flow<List<ViajeEntity>>

    @Query("DELETE FROM VIAJE WHERE idViaje = :idViaje")
    suspend fun eliminarPorId(idViaje: String)

    @Query("SELECT * FROM VIAJE")
    fun obtenerTodos(): Flow<List<ViajeEntity>>

}