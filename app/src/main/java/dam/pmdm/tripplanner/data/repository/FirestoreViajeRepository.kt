package dam.pmdm.tripplanner.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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

    suspend fun crearViaje(viaje: ViajeEntity) {
        val user = auth.currentUser ?: return
        val usuarioDoc = db.collection("usuarios").document(user.uid).get().await()
        val nombre = usuarioDoc.getString("nombre") ?: user.displayName ?: user.email ?: "Usuario"

        val viajeConNombre = viaje.copy(nombrePropietario = nombre)
        coleccionViajes.document(viaje.idViaje).set(viajeConNombre).await()
        viajeDao.insertar(viajeConNombre)

        val email = user.email ?: ""
        val participante = mapOf(
            "idUsuario" to user.uid,
            "nombre" to nombre,
            "email" to email,
            "esAdmin" to true,
            "fechaUnion" to System.currentTimeMillis()
        )

        db.collection("viajes").document(viaje.idViaje)
            .collection("participantes")
            .document(user.uid)
            .set(participante).await()
    }

    suspend fun actualizarViaje(viaje: ViajeEntity) {
        coleccionViajes.document(viaje.idViaje).set(viaje).await()
        viajeDao.actualizar(viaje)
    }

    suspend fun eliminarViaje(idViaje: String) {
        val viajeRef = coleccionViajes.document(idViaje)

        listOf("actividades", "gastos", "participantes", "puntos_interes").forEach { subcoleccion ->
            val docs = viajeRef.collection(subcoleccion).get().await()
            docs.documents.forEach { doc ->
                if (subcoleccion == "gastos") {
                    val repartos = doc.reference.collection("repartos").get().await()
                    repartos.documents.forEach { it.reference.delete().await() }
                }
                doc.reference.delete().await()
            }
        }

        viajeRef.delete().await()
        viajeDao.eliminarPorId(idViaje)
    }

    suspend fun obtenerViajePorIdFirestore(idViaje: String): ViajeEntity? {
        val ahora = System.currentTimeMillis()

        val local = viajeDao.obtenerPorId(idViaje)
        if (local != null) {
            val estado = when {
                ahora < local.fechaInicio -> "PLANIFICADO"
                ahora > local.fechaFin -> "FINALIZADO"
                else -> "EN_CURSO"
            }
            return local.copy(estado = estado)
        }

        return try {
            val doc = coleccionViajes.document(idViaje).get().await()
            doc.toObject(ViajeEntity::class.java)?.let { viaje ->
                val estado = when {
                    ahora < viaje.fechaInicio -> "PLANIFICADO"
                    ahora > viaje.fechaFin -> "FINALIZADO"
                    else -> "EN_CURSO"
                }
                val viajeActualizado = viaje.copy(estado = estado)
                viajeDao.insertar(viajeActualizado)
                viajeActualizado
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun anadirParticipante(idViaje: String, email: String): Result<Unit> {
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

            db.collection("viajes").document(idViaje)
                .update("participantesIds", FieldValue.arrayUnion(idUsuario))
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

    suspend fun eliminarParticipante(idViaje: String, idUsuario: String): Result<Unit> {
        return try {
            db.collection("viajes").document(idViaje)
                .collection("participantes")
                .document(idUsuario)
                .delete().await()

            db.collection("viajes").document(idViaje)
                .update("participantesIds", FieldValue.arrayRemove(idUsuario))
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}