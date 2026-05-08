package dam.pmdm.tripplanner.data.local.dao

import androidx.room.*
import dam.pmdm.tripplanner.data.local.entity.ActividadEntity

/**
 * DAO para la entidad [ActividadEntity].
 * Define las operaciones de acceso a la base de datos local Room
 * para la tabla ACTIVIDAD. Room genera la implementación automáticamente
 * en tiempo de compilación a partir de las anotaciones.
 */
@Dao
interface ActividadDao {

    /**
     * Inserta una actividad en la base de datos local.
     * Si ya existe una actividad con el mismo id, la reemplaza (REPLACE).
     * Se usa REPLACE para sincronizar datos de Firestore sin duplicados.
     *
     * @param actividad Entidad de actividad a insertar o actualizar
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(actividad: ActividadEntity)

    /**
     * Actualiza una actividad existente en la base de datos local.
     *
     * @param actividad Entidad de actividad con los datos actualizados
     */
    @Update
    suspend fun actualizar(actividad: ActividadEntity)

    /**
     * Elimina una actividad de la base de datos local.
     *
     * @param actividad Entidad de actividad a eliminar
     */
    @Delete
    suspend fun eliminar(actividad: ActividadEntity)

    /**
     * Obtiene una actividad por su identificador único.
     *
     * @param idActividad Identificador único de la actividad
     * @return La actividad encontrada o null si no existe
     */
    @Query("SELECT * FROM ACTIVIDAD WHERE idActividad = :idActividad")
    suspend fun obtenerPorId(idActividad: String): ActividadEntity?

    /**
     * Obtiene todas las actividades almacenadas en la base de datos local.
     * Se usa principalmente por [dam.pmdm.tripplanner.NotificacionWorker]
     * para comprobar actividades próximas en las últimas 24 horas.
     *
     * @return Lista con todas las actividades almacenadas localmente
     */
    @Query("SELECT * FROM ACTIVIDAD")
    suspend fun obtenerTodasLasActividades(): List<ActividadEntity>
}