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

/**
 * Repositorio principal que gestiona los viajes y participantes.
 * Implementa una arquitectura híbrida Firestore + Room:
 * - Firestore actúa como fuente de verdad en tiempo real
 * - Room actúa como caché para acceso offline
 *
 * Se elige Firestore como fuente principal porque los viajes son
 * colaborativos — varios participantes pueden modificarlos simultáneamente.
 *
 * @param viajeDao DAO de Room para acceso a la base de datos local
 */
class FirestoreViajeRepository(private val viajeDao: ViajeDao) {

    /** Instancia de Firestore para acceso a la base de datos en la nube */
    private val db = FirebaseFirestore.getInstance()

    /** Instancia de Firebase Auth para obtener el usuario autenticado */
    private val auth = FirebaseAuth.getInstance()

    /**
     * UID del usuario autenticado actualmente.
     * Se obtiene dinámicamente para reflejar cambios de sesión.
     */
    private val idUsuario: String
        get() = auth.currentUser?.uid ?: ""

    /**
     * Referencia a la colección de viajes en Firestore.
     * Se usa como propiedad para simplificar el acceso repetido.
     */
    private val coleccionViajes
        get() = db.collection("viajes")

    /**
     * Escucha en tiempo real los viajes del usuario desde Firestore.
     * Combina dos listeners:
     * 1. Viajes propios (donde el usuario es propietario)
     * 2. Viajes compartidos (donde el usuario es participante)
     *
     * El estado de cada viaje se recalcula dinámicamente en función
     * de las fechas para garantizar que siempre esté actualizado.
     * Se usa [distinctBy] para evitar duplicados cuando el propietario
     * también aparece en la lista de participantes.
     *
     * @return Flow que emite la lista combinada de viajes en tiempo real
     */
    fun sincronizarViajes(): Flow<List<ViajeEntity>> = callbackFlow {
        val ahora = System.currentTimeMillis()
        var viajesPropios = listOf<ViajeEntity>()
        var viajesParticipante = listOf<ViajeEntity>()

        /**
         * Combina y emite los viajes propios y compartidos sin duplicados.
         */
        fun emitirCombinados() {
            val todos = (viajesPropios + viajesParticipante)
                .distinctBy { it.idViaje }
            trySend(todos)
        }

        // Listener para viajes donde el usuario es propietario
        val listenerPropios = coleccionViajes
            .whereEqualTo("idPropietario", idUsuario)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                viajesPropios = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ViajeEntity::class.java)?.let { viaje ->
                        // Recalcular estado dinámicamente según fechas
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

        // Listener para viajes donde el usuario es participante
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

        // Eliminar listeners al cerrar el Flow
        awaitClose {
            listenerPropios.remove()
            listenerParticipante.remove()
        }
    }

    /**
     * Crea un nuevo viaje en Firestore y Room.
     * Pasos realizados:
     * 1. Obtiene el nombre del propietario desde Firestore
     * 2. Guarda el viaje en Firestore y Room
     * 3. Añade al propietario como participante admin automáticamente
     *
     * @param viaje Entidad del viaje a crear
     */
    suspend fun crearViaje(viaje: ViajeEntity) {
        val user = auth.currentUser ?: return

        // Obtener el nombre del propietario desde Firestore
        val usuarioDoc = db.collection("usuarios").document(user.uid).get().await()
        val nombre = usuarioDoc.getString("nombre") ?: user.displayName ?: user.email ?: "Usuario"

        val viajeConNombre = viaje.copy(nombrePropietario = nombre)
        coleccionViajes.document(viaje.idViaje).set(viajeConNombre).await()
        viajeDao.insertar(viajeConNombre)

        // Añadir al propietario como participante admin automáticamente
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

    /**
     * Actualiza un viaje existente en Firestore y Room.
     *
     * @param viaje Entidad del viaje con los datos actualizados
     */
    suspend fun actualizarViaje(viaje: ViajeEntity) {
        coleccionViajes.document(viaje.idViaje).set(viaje).await()
        viajeDao.actualizar(viaje)
    }

    /**
     * Elimina un viaje y todas sus subcolecciones de Firestore y Room.
     * Pasos realizados:
     * 1. Elimina actividades, gastos (con sus repartos), participantes y puntos de interés
     * 2. Elimina el documento del viaje en Firestore
     * 3. Elimina el viaje de Room
     *
     * Se eliminan las subcolecciones manualmente porque Firestore no las
     * elimina automáticamente al borrar el documento padre.
     *
     * @param idViaje Identificador único del viaje a eliminar
     */
    suspend fun eliminarViaje(idViaje: String) {
        val viajeRef = coleccionViajes.document(idViaje)

        // Eliminar todas las subcolecciones del viaje
        listOf("actividades", "gastos", "participantes", "puntos_interes").forEach { subcoleccion ->
            val docs = viajeRef.collection(subcoleccion).get().await()
            docs.documents.forEach { doc ->
                // Los gastos tienen a su vez una subcolección de repartos
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

    /**
     * Obtiene un viaje por su id, primero desde Room y si no existe desde Firestore.
     * Se prioriza Room para una carga más rápida (sin latencia de red).
     * El estado se recalcula dinámicamente en ambos casos.
     *
     * @param idViaje Identificador único del viaje
     * @return El viaje encontrado con el estado actualizado o null si no existe
     */
    suspend fun obtenerViajePorIdFirestore(idViaje: String): ViajeEntity? {
        val ahora = System.currentTimeMillis()

        // Intentar cargar desde Room primero (más rápido)
        val local = viajeDao.obtenerPorId(idViaje)
        if (local != null) {
            val estado = when {
                ahora < local.fechaInicio -> "PLANIFICADO"
                ahora > local.fechaFin -> "FINALIZADO"
                else -> "EN_CURSO"
            }
            return local.copy(estado = estado)
        }

        // Si no está en Room, cargar desde Firestore y cachear
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

    /**
     * Añade un participante a un viaje buscándolo por email.
     * Pasos realizados:
     * 1. Busca el usuario por email en Firestore
     * 2. Añade el usuario a la subcolección de participantes del viaje
     * 3. Añade el UID del usuario al array participantesIds del viaje
     *
     * @param idViaje Identificador del viaje
     * @param email Email del usuario a añadir
     * @return Result con éxito o el error producido
     */
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

            // Añadir el UID al array para facilitar consultas sin índices compuestos
            db.collection("viajes").document(idViaje)
                .update("participantesIds", FieldValue.arrayUnion(idUsuario))
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Escucha en tiempo real los participantes de un viaje desde Firestore.
     *
     * @param idViaje Identificador del viaje
     * @return Flow que emite la lista de participantes en tiempo real
     */
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

    /**
     * Elimina un participante de un viaje.
     * Pasos realizados:
     * 1. Elimina el documento del participante en la subcolección
     * 2. Elimina el UID del array participantesIds del viaje
     *
     * @param idViaje Identificador del viaje
     * @param idUsuario UID del participante a eliminar
     * @return Result con éxito o el error producido
     */
    suspend fun eliminarParticipante(idViaje: String, idUsuario: String): Result<Unit> {
        return try {
            db.collection("viajes").document(idViaje)
                .collection("participantes")
                .document(idUsuario)
                .delete().await()

            // Eliminar el UID del array para mantener la consistencia
            db.collection("viajes").document(idViaje)
                .update("participantesIds", FieldValue.arrayRemove(idUsuario))
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}