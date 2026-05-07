package dam.pmdm.tripplanner.data.local.dao

import androidx.room.*
import dam.pmdm.tripplanner.data.local.entity.UsuarioEntity

@Dao
interface UsuarioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(usuario: UsuarioEntity)

    @Update
    suspend fun actualizar(usuario: UsuarioEntity)

    @Query("SELECT * FROM USUARIO WHERE idUsuario = :idUsuario")
    suspend fun obtenerPorId(idUsuario: String): UsuarioEntity?

}