package dam.pmdm.tripplanner.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dam.pmdm.tripplanner.data.local.TripPlannerDatabase
import dam.pmdm.tripplanner.data.local.entity.UsuarioEntity
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.FirebaseFirestore

class AuthRepository(private val context: Context) {

    private val auth = FirebaseAuth.getInstance()

    val usuarioActual: FirebaseUser?
        get() = auth.currentUser

    val estaAutenticado: Boolean
        get() = auth.currentUser != null

    suspend fun registrar(email: String, password: String, nombre: String): Result<FirebaseUser> {
        return try {
            val resultado = auth.createUserWithEmailAndPassword(email, password).await()
            val user = resultado.user!!
            // Actualizar el nombre en Firebase Auth
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(nombre)
                .build()
            user.updateProfile(profileUpdates).await()
            guardarUsuarioEnRoom(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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

    fun cerrarSesion() {
        auth.signOut()
    }

    private suspend fun guardarUsuarioEnRoom(user: FirebaseUser) {
        val db = TripPlannerDatabase.getInstance(context)
        val usuario = UsuarioEntity(
            idUsuario = user.uid,
            nombre = user.displayName ?: user.email ?: "Usuario",
            email = user.email ?: ""
        )
        db.usuarioDao().insertar(usuario)

        // Guardar también en Firestore para poder buscar por email
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

    suspend fun actualizarPerfil(nombre: String, fotoUrl: String?): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("No hay usuario"))
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(nombre)
                .apply {
                    if (fotoUrl != null) {
                        photoUri = android.net.Uri.parse(fotoUrl)
                    }
                }
                .build()
            user.updateProfile(profileUpdates).await()

            // Actualizar en Firestore
            val db = TripPlannerDatabase.getInstance(context)
            val usuario = db.usuarioDao().obtenerPorId(user.uid)
            if (usuario != null) {
                db.usuarioDao().actualizar(usuario.copy(
                    nombre = nombre,
                    fotoUrl = fotoUrl
                ))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}