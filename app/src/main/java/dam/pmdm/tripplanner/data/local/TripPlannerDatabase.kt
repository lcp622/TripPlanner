package dam.pmdm.tripplanner.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import dam.pmdm.tripplanner.data.local.dao.*
import dam.pmdm.tripplanner.data.local.entity.*

/**
 * Conversores de tipos para Room.
 * Room no soporta nativamente el tipo List<String>, por lo que se necesita
 * convertirlo a String para almacenarlo en SQLite y viceversa.
 * Se usa para serializar el campo [ViajeEntity.participantesIds].
 */
class Converters {

    /**
     * Convierte un String separado por comas a una lista de Strings.
     * Room llama a este método automáticamente al leer datos de la BD.
     *
     * @param value String separado por comas o null
     * @return Lista de Strings sin elementos vacíos
     */
    @Suppress("unused")
    @TypeConverter
    fun fromString(value: String?): List<String> {
        return value?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
    }

    /**
     * Convierte una lista de Strings a un String separado por comas.
     * Room llama a este método automáticamente al escribir datos en la BD.
     *
     * @param list Lista de Strings o null
     * @return String con los elementos separados por comas
     */
    @Suppress("unused")
    @TypeConverter
    fun fromList(list: List<String>?): String {
        return list?.joinToString(",") ?: ""
    }
}

/**
 * Base de datos local de TripPlanner implementada con Room.
 * Contiene todas las entidades de la aplicación y sus DAO correspondientes.
 *
 * Se usa el patrón Singleton para garantizar una única instancia de la BD
 * en toda la aplicación, evitando problemas de concurrencia.
 *
 *
 * Versión actual: 12
 */
@Database(
    entities = [
        UsuarioEntity::class,
        ViajeEntity::class,
        ParticipanteEntity::class,
        ActividadEntity::class,
        GastoEntity::class,
        PuntoInteresEntity::class
    ],
    version = 12,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TripPlannerDatabase : RoomDatabase() {

    /** DAO para operaciones sobre la tabla USUARIO */
    abstract fun usuarioDao(): UsuarioDao

    /** DAO para operaciones sobre la tabla VIAJE */
    abstract fun viajeDao(): ViajeDao

    /** DAO para operaciones sobre la tabla PARTICIPANTE */
    abstract fun participanteDao(): ParticipanteDao

    /** DAO para operaciones sobre la tabla ACTIVIDAD */
    abstract fun actividadDao(): ActividadDao

    /** DAO para operaciones sobre la tabla GASTO */
    abstract fun gastoDao(): GastoDao

    /** DAO para operaciones sobre la tabla PUNTO_INTERES */
    abstract fun puntoInteresDao(): PuntoInteresDao

    companion object {

        /**
         * Instancia única de la base de datos.
         * @Volatile garantiza que los cambios sean visibles entre hilos.
         */
        @Volatile
        private var INSTANCE: TripPlannerDatabase? = null

        /**
         * Obtiene la instancia única de la base de datos.
         * Si no existe, la crea usando el patrón double-checked locking
         * para garantizar thread-safety.
         *
         * @param context Contexto de la aplicación
         * @return Instancia única de [TripPlannerDatabase]
         */
        fun getInstance(context: Context): TripPlannerDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    TripPlannerDatabase::class.java,
                    "tripplanner_db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build().also { INSTANCE = it }
            }
        }
    }
}