package dam.pmdm.tripplanner.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dam.pmdm.tripplanner.data.local.dao.ViajeDao
import dam.pmdm.tripplanner.data.local.entity.ViajeEntity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreViajeRepository(private val viajeDao: ViajeDao) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val idUsuario: String
        get() = auth.currentUser?.uid ?: ""

    private val coleccionViajes
        get() = db.collection("viajes")

    // Sincronizar viajes desde Firestore a Room en tiempo real
    fun sincronizarViajes(): Flow<List<ViajeEntity>> = callbackFlow {
        val listener = coleccionViajes
            .whereEqualTo("idPropietario", idUsuario)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val viajes = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ViajeEntity::class.java)
                } ?: emptyList()
                trySend(viajes)
            }
        awaitClose { listener.remove() }
    }

    // Guardar viaje en Firestore y Room
    suspend fun crearViaje(viaje: ViajeEntity) {
        coleccionViajes.document(viaje.idViaje).set(viaje).await()
        viajeDao.insertar(viaje)
    }

    // Actualizar viaje en Firestore y Room
    suspend fun actualizarViaje(viaje: ViajeEntity) {
        coleccionViajes.document(viaje.idViaje).set(viaje).await()
        viajeDao.actualizar(viaje)
    }

    // Eliminar viaje en Firestore y Room
    suspend fun eliminarViaje(idViaje: String) {
        coleccionViajes.document(idViaje).delete().await()
        viajeDao.eliminarPorId(idViaje)
    }

    suspend fun obtenerViajePorId(idViaje: String): ViajeEntity? {
        return viajeDao.obtenerPorId(idViaje)
    }

    suspend fun obtenerViajePorIdFirestore(idViaje: String): ViajeEntity? {
        return try {
            val doc = coleccionViajes.document(idViaje).get().await()
            doc.toObject(ViajeEntity::class.java)
        } catch (e: Exception) {
            viajeDao.obtenerPorId(idViaje)
        }
    }
}