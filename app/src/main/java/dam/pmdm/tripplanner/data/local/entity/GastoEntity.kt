package dam.pmdm.tripplanner.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "GASTO",
    indices = [Index("idViaje"), Index("idPagador")]
)
data class GastoEntity(
    @PrimaryKey
    val idGasto: String = "",
    val idViaje: String = "",
    val idPagador: String = "",
    val concepto: String = "",
    val importe: Double = 0.0,
    val categoria: String = "OTROS",
    val fecha: Long = 0L,
    val notas: String? = null
)

