package dam.pmdm.tripplanner.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import dam.pmdm.tripplanner.data.local.dao.GastoDao
import dam.pmdm.tripplanner.data.local.entity.GastoEntity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repositorio que gestiona los gastos de un viaje.
 * Implementa una arquitectura híbrida Firestore + Room:
 * - Firestore actúa como fuente de verdad en tiempo real
 * - Room actúa como caché para acceso offline
 *
 * Los repartos de gastos se almacenan únicamente en Firestore
 * como subcolección de cada gasto, sin caché en Room, ya que
 * su consulta siempre requiere conexión para garantizar datos actualizados.
 *
 * @param gastoDao DAO de Room para acceso a la base de datos local
 */
class GastoRepository(private val gastoDao: GastoDao) {

    /** Instancia de Firestore para acceso a la base de datos en la nube */
    private val db = FirebaseFirestore.getInstance()

    /**
     * Referencia a la subcolección de gastos de un viaje en Firestore.
     *
     * @param idViaje Identificador del viaje
     * @return Referencia a la colección de gastos del viaje
     */
    private fun coleccionGastos(idViaje: String) =
        db.collection("viajes").document(idViaje).collection("gastos")

    /**
     * Escucha en tiempo real los gastos de un viaje desde Firestore.
     * Permite que todos los participantes vean los gastos actualizados
     * sin necesidad de recargar manualmente.
     *
     * @param idViaje Identificador del viaje
     * @return Flow que emite la lista de gastos en tiempo real
     */
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

    /**
     * Crea un nuevo gasto en Firestore y lo cachea en Room.
     *
     * @param gasto Entidad del gasto a crear
     */
    suspend fun crearGasto(gasto: GastoEntity) {
        coleccionGastos(gasto.idViaje).document(gasto.idGasto).set(gasto).await()
        gastoDao.insertar(gasto)
    }

    /**
     * Elimina un gasto de Firestore y de Room.
     *
     * @param gasto Entidad del gasto a eliminar
     */
    suspend fun eliminarGasto(gasto: GastoEntity) {
        coleccionGastos(gasto.idViaje).document(gasto.idGasto).delete().await()
        gastoDao.eliminar(gasto)
    }

    /**
     * Actualiza un gasto existente en Firestore y en Room.
     *
     * @param gasto Entidad del gasto con los datos actualizados
     */
    suspend fun actualizarGasto(gasto: GastoEntity) {
        coleccionGastos(gasto.idViaje).document(gasto.idGasto).set(gasto).await()
        gastoDao.actualizar(gasto)
    }

    /**
     * Crea los repartos de un gasto entre los participantes del viaje.
     * El importe se divide equitativamente entre todos los participantes.
     * El pagador no se incluye en el reparto ya que él ya pagó el gasto.
     *
     * Se almacena como subcolección en Firestore con el UID del usuario
     * como id del documento para facilitar las consultas por usuario.
     *
     * @param idViaje Identificador del viaje
     * @param gasto Entidad del gasto a repartir
     * @param participantes Lista de participantes con sus datos
     */
    suspend fun crearRepartoGasto(idViaje: String, gasto: GastoEntity, participantes: List<Map<String, Any>>) {
        // Dividir el importe equitativamente entre todos los participantes
        val importePorPersona = gasto.importe / participantes.size

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

    /**
     * Escucha en tiempo real los repartos de un gasto desde Firestore.
     * Permite mostrar el estado de saldo actualizado a todos los participantes.
     *
     * @param idViaje Identificador del viaje
     * @param idGasto Identificador del gasto
     * @return Flow que emite la lista de repartos en tiempo real
     */
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

    /**
     * Marca un reparto de gasto como saldado.
     * Se usa cuando un participante confirma que ha pagado su parte del gasto.
     *
     * @param idViaje Identificador del viaje
     * @param idGasto Identificador del gasto
     * @param idUsuario UID del usuario que salda su parte
     */
    suspend fun marcarComoSaldado(idViaje: String, idGasto: String, idUsuario: String) {
        db.collection("viajes").document(idViaje)
            .collection("gastos").document(idGasto)
            .collection("repartos").document(idUsuario)
            .update("saldado", true).await()
    }
}