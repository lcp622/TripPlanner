package dam.pmdm.tripplanner.data.local.dao

import androidx.room.*
import dam.pmdm.tripplanner.data.local.entity.PuntoInteresEntity

@Dao
interface PuntoInteresDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(punto: PuntoInteresEntity)

    @Query("DELETE FROM PUNTO_INTERES WHERE idPunto = :idPunto")
    suspend fun eliminarPorId(idPunto: String)

}