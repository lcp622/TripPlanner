package dam.pmdm.tripplanner.data.local.dao

import androidx.room.*
import dam.pmdm.tripplanner.data.local.entity.GastoEntity

/**
 * DAO para la entidad [GastoEntity].
 * Define las operaciones de acceso a la base de datos local Room
 * para la tabla GASTO. Los gastos se sincronizan principalmente
 * desde Firestore — Room actúa como caché para acceso offline.
 */
@Dao
interface GastoDao {

    /**
     * Inserta un gasto en la base de datos local.
     * Si ya existe un gasto con el mismo id, lo reemplaza (REPLACE).
     * Se usa REPLACE para sincronizar datos de Firestore sin duplicados.
     *
     * @param gasto Entidad de gasto a insertar o actualizar
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(gasto: GastoEntity)

    /**
     * Actualiza un gasto existente en la base de datos local.
     *
     * @param gasto Entidad de gasto con los datos actualizados
     */
    @Update
    suspend fun actualizar(gasto: GastoEntity)

    /**
     * Elimina un gasto de la base de datos local.
     *
     * @param gasto Entidad de gasto a eliminar
     */
    @Delete
    suspend fun eliminar(gasto: GastoEntity)

    /**
     * Obtiene un gasto por su identificador único.
     *
     * @param idGasto Identificador único del gasto
     * @return El gasto encontrado o null si no existe
     */
    @Query("SELECT * FROM GASTO WHERE idGasto = :idGasto")
    suspend fun obtenerPorId(idGasto: String): GastoEntity?
}