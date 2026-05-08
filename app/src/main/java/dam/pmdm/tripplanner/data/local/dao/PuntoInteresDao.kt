package dam.pmdm.tripplanner.data.local.dao

import androidx.room.*
import dam.pmdm.tripplanner.data.local.entity.PuntoInteresEntity

/**
 * DAO para la entidad [PuntoInteresEntity].
 * Define las operaciones de acceso a la base de datos local Room
 * para la tabla PUNTO_INTERES.
 *
 * Los puntos de interés se sincronizan desde Firestore al abrir la
 * pantalla de rutas. Room actúa como caché para acceso offline.
 * La consulta de puntos se realiza directamente desde Firestore
 * para garantizar datos actualizados en tiempo real.
 */
@Dao
interface PuntoInteresDao {

    /**
     * Inserta un punto de interés en la base de datos local.
     * Si ya existe un punto con el mismo id, lo reemplaza (REPLACE).
     * Se usa para cachear los puntos obtenidos desde Firestore.
     *
     * @param punto Entidad de punto de interés a insertar o actualizar
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(punto: PuntoInteresEntity)

    /**
     * Elimina un punto de interés de la base de datos local por su id.
     * Se usa junto con la eliminación en Firestore para mantener
     * la sincronización entre la base de datos local y la nube.
     *
     * @param idPunto Identificador único del punto de interés a eliminar
     */
    @Query("DELETE FROM PUNTO_INTERES WHERE idPunto = :idPunto")
    suspend fun eliminarPorId(idPunto: String)
}