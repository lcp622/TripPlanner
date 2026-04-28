package dam.pmdm.tripplanner.data.local.dao

import androidx.room.*
import dam.pmdm.tripplanner.data.local.entity.ParticipanteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParticipanteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(participante: ParticipanteEntity)

    @Delete
    suspend fun eliminar(participante: ParticipanteEntity)

    @Query("SELECT * FROM PARTICIPANTE WHERE idViaje = :idViaje")
    fun obtenerParticipantesPorViaje(idViaje: String): Flow<List<ParticipanteEntity>>

    @Query("SELECT * FROM PARTICIPANTE WHERE idUsuario = :idUsuario")
    fun obtenerViajesPorUsuario(idUsuario: String): Flow<List<ParticipanteEntity>>

    @Query("DELETE FROM PARTICIPANTE WHERE idViaje = :idViaje AND idUsuario = :idUsuario")
    suspend fun eliminarParticipante(idViaje: String, idUsuario: String)

    @Query("SELECT COUNT(*) FROM PARTICIPANTE WHERE idViaje = :idViaje")
    suspend fun contarParticipantes(idViaje: String): Int
}