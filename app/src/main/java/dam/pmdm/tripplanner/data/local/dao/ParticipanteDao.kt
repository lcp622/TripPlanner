package dam.pmdm.tripplanner.data.local.dao

import androidx.room.*
import dam.pmdm.tripplanner.data.local.entity.ParticipanteEntity

@Dao
interface ParticipanteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(participante: ParticipanteEntity)

}