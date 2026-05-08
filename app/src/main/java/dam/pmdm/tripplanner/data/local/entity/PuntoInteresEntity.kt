package dam.pmdm.tripplanner.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room que representa un punto de interés dentro de un viaje.
 * Se almacena en la tabla PUNTO_INTERES de la base de datos local.
 *
 * Los puntos de interés se sincronizan desde Firestore al abrir la
 * pantalla de rutas en [dam.pmdm.tripplanner.ui.viajes.RutasScreen].
 * Room actúa como caché para acceso offline.
 *
 * Se define una clave foránea hacia [ViajeEntity] con CASCADE para que
 * al eliminar un viaje se eliminen automáticamente sus puntos de interés
 * de la base de datos local.
 *
 * Se indexa por [idViaje] para optimizar las consultas de puntos
 * filtrados por viaje.
 *
 * @property idPunto Identificador único del punto de interés
 * @property idViaje Identificador del viaje al que pertenece el punto
 * @property nombre Nombre descriptivo del punto de interés
 * @property categoria Categoría del punto (ATRACCION, RESTAURANTE, HOTEL, MUSEO, MONUMENTO, OTRO)
 * @property latitud Coordenada de latitud del punto en el mapa
 * @property longitud Coordenada de longitud del punto en el mapa
 * @property descripcion Descripción opcional del punto de interés
 * @property orden Orden de visita del punto dentro del itinerario de rutas
 */
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