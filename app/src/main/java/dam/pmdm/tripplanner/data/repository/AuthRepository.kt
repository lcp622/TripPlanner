package dam.pmdm.tripplanner.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dam.pmdm.tripplanner.data.local.TripPlannerDatabase
import dam.pmdm.tripplanner.data.local.entity.UsuarioEntity
import kotlinx.coroutines.tasks.await

class AuthRepository(private val context: Context) {

    private val auth = FirebaseAuth.getInstance()

    val usuarioActual: FirebaseUser?
        get() = auth.currentUser

    val estaAutenticado: Boolean
        get() = auth.currentUser != null

    suspend fun registrar(email: String, password: String): Result<FirebaseUser> {
        return try {
            val resultado = auth.createUserWithEmailAndPassword(email, password).await()
            val user = resultado.user!!
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
    }
}