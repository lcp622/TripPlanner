package dam.pmdm.tripplanner.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "PUNTO_INTERES",
    foreignKeys = [ForeignKey(
        entity = ViajeEntity::class,
        parentColumns = ["idViaje"],
        childColumns = ["idViaje"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("idViaje")]
)
data class PuntoInteresEntity(
    @PrimaryKey
    val idPunto: String,
    val idViaje: String,
    val nombre: String,
    val categoria: String? = null,
    val latitud: Double,
    val longitud: Double,
    val descripcion: String? = null,
    val orden: Int = 0
)