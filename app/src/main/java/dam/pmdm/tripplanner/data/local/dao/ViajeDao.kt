package dam.pmdm.tripplanner.data.local.dao

import androidx.room.*
import dam.pmdm.tripplanner.data.local.entity.ViajeEntity

/**
 * DAO  para la entidad [ViajeEntity].
 * Define las operaciones de acceso a la base de datos local Room
 * para la tabla VIAJE.
 *
 * Los viajes se sincronizan en tiempo real desde Firestore a través de
 * [dam.pmdm.tripplanner.data.repository.FirestoreViajeRepository].
 * Room actúa como caché para permitir el acceso offline a los viajes
 * del usuario. Las consultas de listado se realizan directamente desde
 * Firestore para garantizar datos actualizados en tiempo real.
 */
@Dao
interface ViajeDao {

    /**
     * Inserta un viaje en la base de datos local.
     * Si ya existe un viaje con el mismo id, lo reemplaza (REPLACE).
     * Se usa tanto al crear un viaje nuevo como al sincronizar
     * datos desde Firestore.
     *
     * @param viaje Entidad de viaje a insertar o actualizar
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(viaje: ViajeEntity)

    /**
     * Actualiza un viaje existente en la base de datos local.
     * Se llama tras actualizar el viaje en Firestore para mantener
     * la sincronización entre la nube y el almacenamiento local.
     *
     * @param viaje Entidad de viaje con los datos actualizados
     */
    @Update
    suspend fun actualizar(viaje: ViajeEntity)

    /**
     * Obtiene un viaje por su identificador único.
     * Se usa en [dam.pmdm.tripplanner.data.repository.FirestoreViajeRepository]
     * para comprobar si el viaje ya existe en caché local antes de
     * consultarlo en Firestore, optimizando el tiempo de carga.
     *
     * @param idViaje Identificador único del viaje
     * @return El viaje encontrado o null si no existe en local
     */
    @Query("SELECT * FROM VIAJE WHERE idViaje = :idViaje")
    suspend fun obtenerPorId(idViaje: String): ViajeEntity?

    /**
     * Elimina un viaje de la base de datos local por su id.
     * Se llama tras eliminar el viaje y todas sus subcolecciones
     * en Firestore para mantener la sincronización.
     *
     * @param idViaje Identificador único del viaje a eliminar
     */
    @Query("DELETE FROM VIAJE WHERE idViaje = :idViaje")
    suspend fun eliminarPorId(idViaje: String)
}