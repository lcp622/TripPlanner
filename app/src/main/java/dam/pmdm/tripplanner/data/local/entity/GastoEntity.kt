package dam.pmdm.tripplanner.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "GASTO",
    foreignKeys = [
        ForeignKey(
            entity = ViajeEntity::class,
            parentColumns = ["idViaje"],
            childColumns = ["idViaje"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["idUsuario"],
            childColumns = ["idPagador"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("idViaje"), Index("idPagador")]
)
data class GastoEntity(
    @PrimaryKey
    val idGasto: String,
    val idViaje: String,
    val idPagador: String,
    val concepto: String,
    val importe: Double,
    val categoria: String = "OTROS",
    val fecha: Long,
    val notas: String? = null
)