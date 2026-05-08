package dam.pmdm.tripplanner.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import dam.pmdm.tripplanner.data.local.dao.ActividadDao
import dam.pmdm.tripplanner.data.local.entity.ActividadEntity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Repositorio que gestiona las actividades de un viaje.
 * Implementa una arquitectura híbrida Firestore + Room:
 * - Firestore actúa como fuente de verdad en tiempo real
 * - Room actúa como caché para acceso offline
 *
 * Se elige esta implementación para garantizar que todos los participantes
 * del viaje vean las actividades actualizadas en tiempo real.
 *
 * @param actividadDao DAO de Room para acceso a la base de datos local
 */
class ActividadRepository(private val actividadDao: ActividadDao) {

    /** Instancia de Firestore para acceso a la base de datos en la nube */
    private val db = FirebaseFirestore.getInstance()

    /**
     * Referencia a la subcolección de actividades de un viaje en Firestore.
     *
     * @param idViaje Identificador del viaje
     * @return Referencia a la colección de actividades del viaje
     */
    private fun coleccionActividades(idViaje: String) =
        db.collection("viajes").document(idViaje).collection("actividades")

    /**
     * Escucha en tiempo real las actividades de un viaje desde Firestore.
     * Cada vez que se detecta un cambio en Firestore, se sincronizan
     * las actividades en Room y se emite la lista actualizada.
     *
     * Se usa [callbackFlow] para convertir el listener de Firestore
     * en un Flow de Kotlin compatible con corrutinas.
     *
     * @param idViaje Identificador del viaje
     * @return Flow que emite la lista de actividades en tiempo real
     */
    fun obtenerActividades(idViaje: String): Flow<List<ActividadEntity>> = callbackFlow {
        val listener = coleccionActividades(idViaje)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                // Convertir documentos Firestore a entidades Room
                val actividades = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ActividadEntity::class.java)
                } ?: emptyList()

                // Sincronizar con Room en segundo plano para caché offline
                launch {
                    actividades.forEach { actividadDao.insertar(it) }
                }

                trySend(actividades)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Crea una nueva actividad en Firestore y la cachea en Room.
     * Se guarda primero en Firestore para garantizar la persistencia
     * en la nube antes de actualizar el caché local.
     *
     * @param actividad Entidad de actividad a crear
     */
    suspend fun crearActividad(actividad: ActividadEntity) {
        coleccionActividades(actividad.idViaje)
            .document(actividad.idActividad)
            .set(actividad).await()
        actividadDao.insertar(actividad)
    }

    /**
     * Actualiza una actividad existente en Firestore y en Room.
     *
     * @param actividad Entidad de actividad con los datos actualizados
     */
    suspend fun actualizarActividad(actividad: ActividadEntity) {
        coleccionActividades(actividad.idViaje)
            .document(actividad.idActividad)
            .set(actividad).await()
        actividadDao.actualizar(actividad)
    }

    /**
     * Elimina una actividad de Firestore y de Room.
     *
     * @param actividad Entidad de actividad a eliminar
     */
    suspend fun eliminarActividad(actividad: ActividadEntity) {
        coleccionActividades(actividad.idViaje)
            .document(actividad.idActividad)
            .delete().await()
        actividadDao.eliminar(actividad)
    }

    /**
     * Obtiene una actividad por su id desde Room.
     * Se usa para cargar los datos de una actividad al editarla.
     *
     * @param idActividad Identificador único de la actividad
     * @return La actividad encontrada o null si no existe en local
     */
    suspend fun obtenerPorId(idActividad: String): ActividadEntity? {
        return actividadDao.obtenerPorId(idActividad)
    }
}