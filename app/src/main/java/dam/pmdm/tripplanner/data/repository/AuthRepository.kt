package dam.pmdm.tripplanner.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import androidx.core.net.toUri
import dam.pmdm.tripplanner.data.local.TripPlannerDatabase
import dam.pmdm.tripplanner.data.local.entity.UsuarioEntity
import kotlinx.coroutines.tasks.await

class AuthRepository(private val context: Context) {

    private val auth = FirebaseAuth.getInstance()

    val estaAutenticado: Boolean
        get() = auth.currentUser != null

    suspend fun registrar(email: String, password: String, nombre: String): Result<FirebaseUser> {
        return try {
            val resultado = auth.createUserWithEmailAndPassword(email, password).await()
            val user = resultado.user!!
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

            // Actualizar en Room
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