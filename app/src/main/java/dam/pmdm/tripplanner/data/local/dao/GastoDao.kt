package dam.pmdm.tripplanner.data.local.dao

import androidx.room.*
import dam.pmdm.tripplanner.data.local.entity.GastoEntity

@Dao
interface GastoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(gasto: GastoEntity)

    @Update
    suspend fun actualizar(gasto: GastoEntity)

    @Delete
    suspend fun eliminar(gasto: GastoEntity)


    @Query("SELECT * FROM GASTO WHERE idGasto = :idGasto")
    suspend fun obtenerPorId(idGasto: String): GastoEntity?


}