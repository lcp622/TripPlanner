package dam.pmdm.tripplanner.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room que representa un usuario de la aplicación.
 * Se almacena en la tabla USUARIO de la base de datos local.
 *
 * Los datos del usuario se obtienen desde Firebase Auth y Firestore
 * al iniciar sesión o registrarse, y se cachean en Room a través de
 * [dam.pmdm.tripplanner.data.repository.AuthRepository].
 * Esto permite mostrar los datos del usuario sin conexión.
 *
 * @property idUsuario Identificador único del usuario
 * @property nombre Nombre visible del usuario
 * @property email Correo electrónico del usuario
 * @property fotoUrl URL de la foto de perfil del usuario
 * @property fechaRegistro Fecha de registro en milisegundos
 */
@Entity(tableName = "USUARIO")
data class UsuarioEntity(
    @PrimaryKey
    @ColumnInfo(name = "idUsuario")
    val idUsuario: String,
    val nombre: String,
    val email: String,
    val fotoUrl: String? = null,
    val fechaRegistro: Long = System.currentTimeMillis()
)