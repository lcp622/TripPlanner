package dam.pmdm.tripplanner.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

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