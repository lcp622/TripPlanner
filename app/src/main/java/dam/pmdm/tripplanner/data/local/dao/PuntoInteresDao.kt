package dam.pmdm.tripplanner.data.local.dao

import androidx.room.*
import dam.pmdm.tripplanner.data.local.entity.PuntoInteresEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PuntoInteresDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(punto: PuntoInteresEntity)

    @Update
    suspend fun actualizar(punto: PuntoInteresEntity)

    @Delete
    suspend fun eliminar(punto: PuntoInteresEntity)

    @Query("SELECT * FROM PUNTO_INTERES WHERE idRuta = :idRuta ORDER BY orden ASC")
    fun obtenerPuntosPorRuta(idRuta: String): Flow<List<PuntoInteresEntity>>

    @Query("DELETE FROM PUNTO_INTERES WHERE idRuta = :idRuta")
    suspend fun eliminarPorRuta(idRuta: String)

    @Query("UPDATE PUNTO_INTERES SET orden = :nuevoOrden WHERE idPunto = :idPunto")
    suspend fun actualizarOrden(idPunto: String, nuevoOrden: Int)
}