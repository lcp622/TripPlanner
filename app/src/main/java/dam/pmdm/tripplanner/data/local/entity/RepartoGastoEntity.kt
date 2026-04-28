package dam.pmdm.tripplanner.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "REPARTO_GASTO",
    foreignKeys = [
        ForeignKey(
            entity = GastoEntity::class,
            parentColumns = ["idGasto"],
            childColumns = ["idGasto"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["idUsuario"],
            childColumns = ["idUsuario"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("idGasto"), Index("idUsuario")]
)
data class RepartoGastoEntity(
    @PrimaryKey
    val idReparto: String,
    val idGasto: String,
    val idUsuario: String,
    val importeAsignado: Double,
    val saldado: Boolean = false
)