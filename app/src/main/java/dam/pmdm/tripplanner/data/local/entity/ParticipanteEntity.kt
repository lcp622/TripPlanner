package dam.pmdm.tripplanner.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room que representa un participante dentro de un viaje.
 * Se almacena en la tabla PARTICIPANTE de la base de datos local.
 *
 * Los participantes se sincronizan desde Firestore a través de
 * [dam.pmdm.tripplanner.ui.viajes.ParticipantesScreen].
 * Room actúa como caché para acceso offline.
 *
 * Se define una clave foránea hacia [ViajeEntity] con CASCADE para que
 * al eliminar un viaje se eliminen automáticamente sus participantes
 * de la base de datos local.
 *
 * Se indexa por [idViaje] e [idUsuario] para optimizar las consultas
 * de participantes filtrados por viaje y por usuario.
 *
 * @property idParticipante Identificador único del participante
 * @property idViaje Identificador del viaje al que pertenece el participante
 * @property idUsuario Identificador del usuario participante
 * @property fechaUnion Fecha en que el usuario se unió al viaje en milisegundos
 * @property esAdmin Indica si el participante es administrador del viaje
 */
@Entity(
    tableName = "PARTICIPANTE",
    foreignKeys = [
        ForeignKey(
            entity = ViajeEntity::class,
            parentColumns = ["idViaje"],
            childColumns = ["idViaje"],
            onDelete = ForeignKey.CASCADE
        )
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