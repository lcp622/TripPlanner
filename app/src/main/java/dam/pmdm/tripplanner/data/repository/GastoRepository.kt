package dam.pmdm.tripplanner.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import dam.pmdm.tripplanner.data.local.dao.GastoDao
import dam.pmdm.tripplanner.data.local.entity.GastoEntity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class GastoRepository(private val gastoDao: GastoDao) {

    private val db = FirebaseFirestore.getInstance()

    private fun coleccionGastos(idViaje: String) =
        db.collection("viajes").document(idViaje).collection("gastos")

    fun obtenerGastos(idViaje: String): Flow<List<GastoEntity>> = callbackFlow {
        val listener = coleccionGastos(idViaje)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val gastos = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(GastoEntity::class.java)
                } ?: emptyList()
                trySend(gastos)
            }
        awaitClose { listener.remove() }
    }

    suspend fun crearGasto(gasto: GastoEntity) {
        coleccionGastos(gasto.idViaje).document(gasto.idGasto).set(gasto).await()
        gastoDao.insertar(gasto)
    }

    suspend fun eliminarGasto(gasto: GastoEntity) {
        coleccionGastos(gasto.idViaje).document(gasto.idGasto).delete().await()
        gastoDao.eliminar(gasto)
    }

    suspend fun actualizarGasto(gasto: GastoEntity) {
        coleccionGastos(gasto.idViaje).document(gasto.idGasto).set(gasto).await()
        gastoDao.actualizar(gasto)
    }

    fun obtenerTodosLosGastos(idViajes: List<String>): Flow<List<GastoEntity>> = callbackFlow {
        if (idViajes.isEmpty()) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val listener = db.collection("viajes")
            .whereIn("idViaje", idViajes)
            .addSnapshotListener { _, _ -> }

        // Obtenemos los gastos de cada viaje
        val listeners = idViajes.map { idViaje ->
            coleccionGastos(idViaje).addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val gastos = snapshot?.documents?.mapNotNull { it.toObject(GastoEntity::class.java) } ?: emptyList()
                trySend(gastos)
            }
        }

        awaitClose {
            listener.remove()
            listeners.forEach { it.remove() }
        }
    }

    suspend fun crearRepartoGasto(idViaje: String, gasto: GastoEntity, participantes: List<Map<String, Any>>) {
        val importePorPersona = gasto.importe / participantes.size // +1 por el pagador

        participantes.forEach { participante ->
            val idUsuario = participante["idUsuario"]?.toString() ?: return@forEach
            // El pagador no se debe a sí mismo
            if (idUsuario == gasto.idPagador) return@forEach

            val reparto = mapOf(
                "idReparto" to java.util.UUID.randomUUID().toString(),
                "idGasto" to gasto.idGasto,
                "idViaje" to idViaje,
                "idUsuario" to idUsuario,
                "nombreUsuario" to (participante["nombre"]?.toString() ?: ""),
                "importeAsignado" to importePorPersona,
                "saldado" to false
            )

            db.collection("viajes").document(idViaje)
                .collection("gastos").document(gasto.idGasto)
                .collection("repartos").document(idUsuario)
                .set(reparto).await()
        }
    }

    fun obtenerRepartos(idViaje: String, idGasto: String): Flow<List<Map<String, Any>>> = callbackFlow {
        val listener = db.collection("viajes").document(idViaje)
            .collection("gastos").document(idGasto)
            .collection("repartos")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val repartos = snapshot?.documents?.mapNotNull { it.data } ?: emptyList()
                trySend(repartos)
            }
        awaitClose { listener.remove() }
    }

    suspend fun marcarComoSaldado(idViaje: String, idGasto: String, idUsuario: String) {
        db.collection("viajes").document(idViaje)
            .collection("gastos").document(idGasto)
            .collection("repartos").document(idUsuario)
            .update("saldado", true).await()
    }
}