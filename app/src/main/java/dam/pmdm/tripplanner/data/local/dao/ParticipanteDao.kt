package dam.pmdm.tripplanner.data.local.dao

import androidx.room.*
import dam.pmdm.tripplanner.data.local.entity.ParticipanteEntity

/**
 * DAO para la entidad [ParticipanteEntity].
 * Define las operaciones de acceso a la base de datos local Room
 * para la tabla PARTICIPANTE.
 *
 * Los participantes se gestionan principalmente desde Firestore a través
 * de [dam.pmdm.tripplanner.data.repository.FirestoreViajeRepository].
 * Room actúa como caché para permitir la consulta offline de participantes.
 * Las operaciones de eliminación se realizan directamente en Firestore,
 * por lo que no se necesita un método eliminar en este DAO.
 */
@Dao
interface ParticipanteDao {

    /**
     * Inserta un participante en la base de datos local.
     * Si ya existe un participante con el mismo id, lo reemplaza (REPLACE).
     * Se usa para cachear los participantes obtenidos desde Firestore.
     *
     * @param participante Entidad de participante a insertar o actualizar
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(participante: ParticipanteEntity)
}