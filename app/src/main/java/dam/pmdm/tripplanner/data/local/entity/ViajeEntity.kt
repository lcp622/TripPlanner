package dam.pmdm.tripplanner.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "VIAJE",
    indices = [Index("idPropietario"), Index("estado")]
)
data class ViajeEntity(
    @PrimaryKey
    val idViaje: String = "",
    val nombre: String = "",
    val paisDestino: String = "",
    val fechaInicio: Long = 0L,
    val fechaFin: Long = 0L,
    val descripcion: String? = null,
    val presupuestoTotal: Double = 0.0,
    val estado: String = "PLANIFICADO",
    val idPropietario: String = ""
)