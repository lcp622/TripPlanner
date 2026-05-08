package dam.pmdm.tripplanner.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room que representa una actividad dentro de un viaje.
 * Se almacena en la tabla ACTIVIDAD de la base de datos local.
 *
 * Las actividades se sincronizan en tiempo real desde Firestore
 * a través de [dam.pmdm.tripplanner.data.repository.ActividadRepository].
 * Room actúa como caché para acceso offline.
 *
 * Se indexa por [idViaje] para optimizar las consultas de actividades
 * filtradas por viaje.
 *
 * @property idActividad Identificador único de la actividad
 * @property idViaje Identificador del viaje al que pertenece la actividad
 * @property titulo Título descriptivo de la actividad
 * @property descripcion Descripción opcional de la actividad
 * @property fecha Fecha de la actividad en milisegundos
 * @property horaInicio Hora de inicio en formato "HH:mm"
 * @property horaFin Hora de fin en formato "HH:mm"
 * @property lugar Lugar donde se realiza la actividad
 */
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