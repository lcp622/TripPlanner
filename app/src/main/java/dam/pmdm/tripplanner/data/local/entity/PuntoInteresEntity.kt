package dam.pmdm.tripplanner.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "PUNTO_INTERES",
    foreignKeys = [ForeignKey(
        entity = RutaEntity::class,
        parentColumns = ["idRuta"],
        childColumns = ["idRuta"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("idRuta")]
)
data class PuntoInteresEntity(
    @PrimaryKey
    val idPunto: String,
    val idRuta: String,
    val nombre: String,
    val categoria: String? = null,
    val latitud: Double,
    val longitud: Double,
    val descripcion: String? = null,
    val orden: Int = 0
)