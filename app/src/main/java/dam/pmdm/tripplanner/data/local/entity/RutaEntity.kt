package dam.pmdm.tripplanner.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "RUTA",
    foreignKeys = [ForeignKey(
        entity = ViajeEntity::class,
        parentColumns = ["idViaje"],
        childColumns = ["idViaje"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("idViaje")]
)
data class RutaEntity(
    @PrimaryKey
    val idRuta: String,
    val idViaje: String,
    val nombre: String,
    val descripcion: String? = null,
    val fechaCreacion: Long = System.currentTimeMillis()
)