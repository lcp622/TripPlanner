package dam.pmdm.tripplanner.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import dam.pmdm.tripplanner.data.local.dao.*
import dam.pmdm.tripplanner.data.local.entity.*

class Converters {
    @Suppress("unused")
    @TypeConverter
    fun fromString(value: String?): List<String> {
        return value?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
    }

    @Suppress("unused")
    @TypeConverter
    fun fromList(list: List<String>?): String {
        return list?.joinToString(",") ?: ""
    }
}

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

    abstract fun usuarioDao(): UsuarioDao
    abstract fun viajeDao(): ViajeDao
    abstract fun participanteDao(): ParticipanteDao
    abstract fun actividadDao(): ActividadDao
    abstract fun gastoDao(): GastoDao
    abstract fun puntoInteresDao(): PuntoInteresDao

    companion object {
        @Volatile
        private var INSTANCE: TripPlannerDatabase? = null

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