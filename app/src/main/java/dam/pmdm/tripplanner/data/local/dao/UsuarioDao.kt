package dam.pmdm.tripplanner.data.local.dao

import androidx.room.*
import dam.pmdm.tripplanner.data.local.entity.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(usuario: UsuarioEntity)

    @Update
    suspend fun actualizar(usuario: UsuarioEntity)

    @Delete
    suspend fun eliminar(usuario: UsuarioEntity)

    @Query("SELECT * FROM USUARIO WHERE idUsuario = :idUsuario")
    suspend fun obtenerPorId(idUsuario: String): UsuarioEntity?

    @Query("SELECT * FROM USUARIO WHERE email = :email")
    suspend fun obtenerPorEmail(email: String): UsuarioEntity?
}