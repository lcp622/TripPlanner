package dam.pmdm.tripplanner.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import dam.pmdm.tripplanner.data.local.dao.ActividadDao
import dam.pmdm.tripplanner.data.local.entity.ActividadEntity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

class ActividadRepository(private val actividadDao: ActividadDao) {

    private val db = FirebaseFirestore.getInstance()
    private fun coleccionActividades(idViaje: String) =
        db.collection("viajes").document(idViaje).collection("actividades")

    fun obtenerActividades(idViaje: String): Flow<List<ActividadEntity>> = callbackFlow {
        val listener = coleccionActividades(idViaje)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val actividades = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ActividadEntity::class.java)
                } ?: emptyList()
                launch {
                    actividades.forEach { actividadDao.insertar(it) }
                }
                trySend(actividades)
            }
        awaitClose { listener.remove() }
    }

    suspend fun crearActividad(actividad: ActividadEntity) {
        coleccionActividades(actividad.idViaje)
            .document(actividad.idActividad)
            .set(actividad).await()
        actividadDao.insertar(actividad)
    }

    suspend fun actualizarActividad(actividad: ActividadEntity) {
        coleccionActividades(actividad.idViaje)
            .document(actividad.idActividad)
            .set(actividad).await()
        actividadDao.actualizar(actividad)
    }

    suspend fun eliminarActividad(actividad: ActividadEntity) {
        coleccionActividades(actividad.idViaje)
            .document(actividad.idActividad)
            .delete().await()
        actividadDao.eliminar(actividad)
    }

    suspend fun obtenerPorId(idActividad: String): ActividadEntity? {
        return actividadDao.obtenerPorId(idActividad)
    }
}