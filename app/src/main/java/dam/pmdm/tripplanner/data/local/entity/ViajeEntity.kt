package dam.pmdm.tripplanner.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room que representa un viaje de la aplicación.
 * Se almacena en la tabla VIAJE de la base de datos local.
 *
 * Los viajes se sincronizan en tiempo real desde Firestore a través de
 * [dam.pmdm.tripplanner.data.repository.FirestoreViajeRepository].
 * Room actúa como caché para permitir el acceso offline.
 *
 * Se indexa por [idPropietario] y [estado] para optimizar las consultas
 * de viajes filtrados por usuario y por estado.
 *
 * El campo [participantesIds] almacena los UID de todos los participantes
 * del viaje como lista — se usa en Firestore para consultar los viajes
 * donde el usuario es participante sin necesidad de índices compuestos.
 * Room serializa esta lista usando [dam.pmdm.tripplanner.data.local.Converters].
 *
 * El estado del viaje se recalcula dinámicamente en función de las fechas
 * cada vez que se carga desde Firestore, no se persiste el estado calculado.
 *
 * @property idViaje Identificador único del viaje
 * @property nombre Nombre descriptivo del viaje
 * @property paisDestino País o destino del viaje
 * @property fechaInicio Fecha de inicio del viaje en milisegundos
 * @property fechaFin Fecha de fin del viaje en milisegundos
 * @property descripcion Descripción opcional del viaje
 * @property presupuestoTotal Presupuesto total del viaje en euros
 * @property estado Estado del viaje: PLANIFICADO, EN_CURSO o FINALIZADO
 * @property idPropietario UID de Firebase del usuario que creó el viaje
 * @property nombrePropietario Nombre del propietario para mostrar sin consultas adicionales
 * @property participantesIds Lista de UID de todos los participantes del viaje
 */
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
    val idPropietario: String = "",
    val nombrePropietario: String = "",
    val participantesIds: List<String> = emptyList()
)