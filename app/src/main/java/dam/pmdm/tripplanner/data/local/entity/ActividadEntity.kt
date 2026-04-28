package dam.pmdm.tripplanner.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ACTIVIDAD",
    foreignKeys = [ForeignKey(
        entity = ViajeEntity::class,
        parentColumns = ["idViaje"],
        childColumns = ["idViaje"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("idViaje")]
)
data class ActividadEntity(
    @PrimaryKey
    val idActividad: String,
    val idViaje: String,
    val titulo: String,
    val descripcion: String? = null,
    val fecha: Long,
    val horaInicio: String? = null,
    val horaFin: String? = null,
    val lugar: String? = null
)