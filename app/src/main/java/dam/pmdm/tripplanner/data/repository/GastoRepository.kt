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
}