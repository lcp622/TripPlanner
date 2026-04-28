package dam.pmdm.tripplanner.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ACTIVIDAD",
    indices = [Index("idViaje")]
)
data class ActividadEntity(
    @PrimaryKey
    val idActividad: String = "",
    val idViaje: String = "",
    val titulo: String = "",
    val descripcion: String? = null,
    val fecha: Long = 0L,
    val horaInicio: String? = null,
    val horaFin: String? = null,
    val lugar: String? = null
)