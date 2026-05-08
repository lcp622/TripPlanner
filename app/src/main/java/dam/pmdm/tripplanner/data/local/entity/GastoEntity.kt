package dam.pmdm.tripplanner.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room que representa un gasto dentro de un viaje.
 * Se almacena en la tabla GASTO de la base de datos local.
 *
 * Los gastos se sincronizan en tiempo real desde Firestore
 * a través de [dam.pmdm.tripplanner.data.repository.GastoRepository].
 * Room actúa como caché para acceso offline.
 *
 * Se indexa por [idViaje] e [idPagador] para optimizar las consultas
 * de gastos filtrados por viaje y por usuario pagador.
 *
 * Los repartos de cada gasto se almacenan únicamente en Firestore
 * como subcolección, no en Room.
 *
 * @property idGasto Identificador único del gasto
 * @property idViaje Identificador del viaje al que pertenece el gasto
 * @property idPagador Identificador del usuario que pagó el gasto
 * @property concepto Descripción del concepto del gasto
 * @property importe Importe total del gasto en euros
 * @property categoria Categoría del gasto (ALOJAMIENTO, TRANSPORTE, COMIDA, OCIO, OTROS)
 * @property fecha Fecha del gasto en milisegundos
 * @property notas Notas adicionales opcionales sobre el gasto
 * @property nombrePagador Nombre del usuario que pagó, almacenado para acceso offline
 */
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
    val notas: String? = null,
    val nombrePagador: String = ""
)