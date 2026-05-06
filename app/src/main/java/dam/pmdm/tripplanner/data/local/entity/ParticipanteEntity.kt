package dam.pmdm.tripplanner.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "PARTICIPANTE",
    foreignKeys = [
        ForeignKey(
            entity = ViajeEntity::class,
            parentColumns = ["idViaje"],
            childColumns = ["idViaje"],
            onDelete = ForeignKey.CASCADE
        )
        // Quitamos la FK a UsuarioEntity porque el participante puede no estar en Room
    ],
    indices = [Index("idViaje"), Index("idUsuario")]
)
data class ParticipanteEntity(
    @PrimaryKey
    val idParticipante: String,
    val idViaje: String,
    val idUsuario: String,
    val fechaUnion: Long = System.currentTimeMillis(),
    val esAdmin: Boolean = false
)