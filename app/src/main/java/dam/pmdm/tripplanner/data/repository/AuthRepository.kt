package dam.pmdm.tripplanner.data.repository

import android.content.Context
import androidx.core.net.toUri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import dam.pmdm.tripplanner.data.local.TripPlannerDatabase
import dam.pmdm.tripplanner.data.local.entity.UsuarioEntity
import kotlinx.coroutines.tasks.await

/**
 * Repositorio que gestiona la autenticación de usuarios.
 * Coordina las operaciones entre Firebase Auth, Firestore y Room:
 * - Firebase Auth gestiona el registro, login y cierre de sesión
 * - Firestore almacena los datos del usuario en la nube
 * - Room cachea los datos del usuario para acceso offline
 *
 * @param context Contexto de la aplicación para acceder a Room
 */
class AuthRepository(private val context: Context) {

    /** Instancia de Firebase Auth para gestión de autenticación */
    private val auth = FirebaseAuth.getInstance()

    /**
     * Indica si hay un usuario autenticado actualmente.
     * @return true si hay un usuario con sesión activa, false en caso contrario
     */
    val estaAutenticado: Boolean
        get() = auth.currentUser != null

    /**
     * Registra un nuevo usuario con email y contraseña.
     * Pasos realizados:
     * 1. Crea el usuario en Firebase Auth
     * 2. Actualiza el nombre en el perfil de Firebase Auth
     * 3. Guarda los datos en Room y Firestore
     *
     * @param email Correo electrónico del usuario
     * @param password Contraseña del usuario
     * @param nombre Nombre visible del usuario
     * @return Result con el usuario creado o el error producido
     */
    suspend fun registrar(email: String, password: String, nombre: String): Result<FirebaseUser> {
        return try {
            val resultado = auth.createUserWithEmailAndPassword(email, password).await()
            val user = resultado.user!!

            // Actualizar el nombre en el perfil de Firebase Auth
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(nombre)
                .build()
            user.updateProfile(profileUpdates).await()

            guardarUsuarioEnRoom(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Inicia sesión con email y contraseña.
     * Tras el login exitoso, guarda los datos del usuario en Room
     * para permitir acceso offline.
     *
     * @param email Correo electrónico del usuario
     * @param password Contraseña del usuario
     * @return Result con el usuario autenticado o el error producido
     */
    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            val resultado = auth.signInWithEmailAndPassword(email, password).await()
            val user = resultado.user!!
            guardarUsuarioEnRoom(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cierra la sesión del usuario actual en Firebase Auth.
     */
    fun cerrarSesion() {
        auth.signOut()
    }

    /**
     * Guarda los datos del usuario en Room y Firestore.
     * Se llama tanto al registrarse como al iniciar sesión.
     * Se elige guardar en ambos para garantizar disponibilidad offline
     * y sincronización con otros participantes del viaje.
     *
     * @param user Usuario autenticado de Firebase Auth
     */
    private suspend fun guardarUsuarioEnRoom(user: FirebaseUser) {
        // Cachear en Room para acceso offline
        val db = TripPlannerDatabase.getInstance(context)
        val usuario = UsuarioEntity(
            idUsuario = user.uid,
            nombre = user.displayName ?: user.email ?: "Usuario",
            email = user.email ?: ""
        )
        db.usuarioDao().insertar(usuario)

        // Guardar en Firestore para que otros usuarios puedan encontrarlo por email
        val firestore = FirebaseFirestore.getInstance()
        val usuarioMap = mapOf(
            "idUsuario" to user.uid,
            "nombre" to (user.displayName ?: user.email ?: "Usuario"),
            "email" to (user.email ?: "")
        )
        firestore.collection("usuarios")
            .document(user.uid)
            .set(usuarioMap)
            .await()
    }

    /**
     * Actualiza el perfil del usuario autenticado.
     * Pasos realizados:
     * 1. Actualiza el nombre y foto en Firebase Auth
     * 2. Actualiza los datos en Firestore
     * 3. Actualiza el caché en Room
     *
     * @param nombre Nuevo nombre del usuario
     * @param fotoUrl Nueva URL de la foto de perfil (null para no cambiarla)
     * @return Result con éxito o el error producido
     */
    suspend fun actualizarPerfil(nombre: String, fotoUrl: String?): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("No hay usuario"))

            // Actualizar nombre y foto en Firebase Auth
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(nombre)
                .apply {
                    if (fotoUrl != null) {
                        photoUri = fotoUrl.toUri()
                    }
                }
                .build()
            user.updateProfile(profileUpdates).await()

            // Actualizar en Firestore
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("usuarios").document(user.uid)
                .update("nombre", nombre, "fotoUrl", fotoUrl)
                .await()

            // Actualizar caché en Room usando insertar con REPLACE
            val db = TripPlannerDatabase.getInstance(context)
            val usuario = UsuarioEntity(
                idUsuario = user.uid,
                nombre = nombre,
                email = user.email ?: "",
                fotoUrl = fotoUrl
            )
            db.usuarioDao().insertar(usuario)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}