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

    fun sincronizarViajes(): Flow<List<ViajeEntity>> = callbackFlow {
        val ahora = System.currentTimeMillis()
        var viajesPropios = listOf<ViajeEntity>()
        var viajesParticipante = listOf<ViajeEntity>()

        fun emitirCombinados() {
            val todos = (viajesPropios + viajesParticipante)
                .distinctBy { it.idViaje }
            trySend(todos)
        }

        // Viajes propios
        val listenerPropios = coleccionViajes
            .whereEqualTo("idPropietario", idUsuario)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                viajesPropios = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ViajeEntity::class.java)?.let { viaje ->
                        val estado = when {
                            ahora < viaje.fechaInicio -> "PLANIFICADO"
                            ahora > viaje.fechaFin -> "FINALIZADO"
                            else -> "EN_CURSO"
                        }
                        viaje.copy(estado = estado)
                    }
                } ?: emptyList()
                emitirCombinados()
            }

        // Viajes donde es participante
        val listenerParticipante = coleccionViajes
            .whereArrayContains("participantesIds", idUsuario)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                viajesParticipante = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ViajeEntity::class.java)?.let { viaje ->
                        val estado = when {
                            ahora < viaje.fechaInicio -> "PLANIFICADO"
                            ahora > viaje.fechaFin -> "FINALIZADO"
                            else -> "EN_CURSO"
                        }
                        viaje.copy(estado = estado)
                    }
                } ?: emptyList()
                emitirCombinados()
            }

        awaitClose {
            listenerPropios.remove()
            listenerParticipante.remove()
        }
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
            val ahora = System.currentTimeMillis()
            doc.toObject(ViajeEntity::class.java)?.let { viaje ->
                val estado = when {
                    ahora < viaje.fechaInicio -> "PLANIFICADO"
                    ahora > viaje.fechaFin -> "FINALIZADO"
                    else -> "EN_CURSO"
                }
                viaje.copy(estado = estado)
            }
        } catch (e: Exception) {
            viajeDao.obtenerPorId(idViaje)
        }
    }

    suspend fun añadirParticipante(idViaje: String, email: String): Result<Unit> {
        return try {
            val usuarioSnapshot = db.collection("usuarios")
                .whereEqualTo("email", email)
                .get().await()

            if (usuarioSnapshot.isEmpty) {
                return Result.failure(Exception("No se encontró ningún usuario con ese email"))
            }

            val idUsuario = usuarioSnapshot.documents.first().id
            val nombreUsuario = usuarioSnapshot.documents.first().getString("nombre") ?: email

            val participante = mapOf(
                "idUsuario" to idUsuario,
                "nombre" to nombreUsuario,
                "email" to email,
                "esAdmin" to false,
                "fechaUnion" to System.currentTimeMillis()
            )

            db.collection("viajes").document(idViaje)
                .collection("participantes")
                .document(idUsuario)
                .set(participante).await()

            // Añadir idUsuario al array participantesIds del viaje
            db.collection("viajes").document(idViaje)
                .update("participantesIds", com.google.firebase.firestore.FieldValue.arrayUnion(idUsuario))
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun obtenerParticipantes(idViaje: String): Flow<List<Map<String, Any>>> = callbackFlow {
        val listener = db.collection("viajes").document(idViaje)
            .collection("participantes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val participantes = snapshot?.documents?.mapNotNull { it.data } ?: emptyList()
                trySend(participantes)
            }
        awaitClose { listener.remove() }
    }
}