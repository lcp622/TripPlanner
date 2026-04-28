package dam.pmdm.tripplanner.data.local.dao

import androidx.room.*
import dam.pmdm.tripplanner.data.local.entity.RepartoGastoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RepartoGastoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(reparto: RepartoGastoEntity)

    @Update
    suspend fun actualizar(reparto: RepartoGastoEntity)

    @Delete
    suspend fun eliminar(reparto: RepartoGastoEntity)

    @Query("SELECT * FROM REPARTO_GASTO WHERE idGasto = :idGasto")
    fun obtenerRepartosPorGasto(idGasto: String): Flow<List<RepartoGastoEntity>>

    @Query("SELECT * FROM REPARTO_GASTO WHERE idUsuario = :idUsuario AND saldado = 0")
    fun obtenerDeudasPendientes(idUsuario: String): Flow<List<RepartoGastoEntity>>

    @Query("UPDATE REPARTO_GASTO SET saldado = 1 WHERE idReparto = :idReparto")
    suspend fun marcarComoSaldado(idReparto: String)

    @Query("DELETE FROM REPARTO_GASTO WHERE idGasto = :idGasto")
    suspend fun eliminarPorGasto(idGasto: String)
}