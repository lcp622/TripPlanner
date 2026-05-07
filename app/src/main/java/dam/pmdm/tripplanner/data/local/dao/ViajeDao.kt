package dam.pmdm.tripplanner.data.local.dao

import androidx.room.*
import dam.pmdm.tripplanner.data.local.entity.ViajeEntity

@Dao
interface ViajeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(viaje: ViajeEntity)

    @Update
    suspend fun actualizar(viaje: ViajeEntity)

    @Query("SELECT * FROM VIAJE WHERE idViaje = :idViaje")
    suspend fun obtenerPorId(idViaje: String): ViajeEntity?

    @Query("DELETE FROM VIAJE WHERE idViaje = :idViaje")
    suspend fun eliminarPorId(idViaje: String)


}