package dam.pmdm.tripplanner.data.local.dao

import androidx.room.*
import dam.pmdm.tripplanner.data.local.entity.UsuarioEntity

/**
 * DAO para la entidad [UsuarioEntity].
 * Define las operaciones de acceso a la base de datos local Room
 * para la tabla USUARIO.
 *
 * Los datos del usuario se obtienen desde Firebase Auth y Firestore,
 * y se cachean en Room al iniciar sesión o registrarse.
 * La eliminación del usuario se gestiona directamente desde Firebase Auth
 * y Firestore en [dam.pmdm.tripplanner.ui.perfil.PerfilScreen],
 * por lo que no se necesita un método eliminar en este DAO.
 */
@Dao
interface UsuarioDao {

    /**
     * Inserta un usuario en la base de datos local.
     * Si ya existe un usuario con el mismo id, lo reemplaza (REPLACE).
     * Se usa al iniciar sesión o registrarse para cachear los datos
     * del usuario autenticado y permitir acceso offline.
     *
     * @param usuario Entidad de usuario a insertar o actualizar
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(usuario: UsuarioEntity)
}