package dam.pmdm.tripplanner.data.local.dao

import androidx.room.*
import dam.pmdm.tripplanner.data.local.entity.GastoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GastoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(gasto: GastoEntity)

    @Update
    suspend fun actualizar(gasto: GastoEntity)

    @Delete
    suspend fun eliminar(gasto: GastoEntity)

    @Query("SELECT * FROM GASTO WHERE idViaje = :idViaje ORDER BY fecha DESC")
    fun obtenerGastosPorViaje(idViaje: String): Flow<List<GastoEntity>>

    @Query("SELECT * FROM GASTO WHERE idViaje = :idViaje AND categoria = :categoria ORDER BY fecha DESC")
    fun obtenerPorCategoria(idViaje: String, categoria: String): Flow<List<GastoEntity>>

    @Query("SELECT SUM(importe) FROM GASTO WHERE idViaje = :idViaje")
    fun obtenerTotalGastos(idViaje: String): Flow<Double?>

    @Query("SELECT * FROM GASTO WHERE idGasto = :idGasto")
    suspend fun obtenerPorId(idGasto: String): GastoEntity?

    @Query("DELETE FROM GASTO WHERE idViaje = :idViaje")
    suspend fun eliminarPorViaje(idViaje: String)


}