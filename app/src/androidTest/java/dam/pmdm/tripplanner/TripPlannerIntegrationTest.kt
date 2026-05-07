package dam.pmdm.tripplanner

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dam.pmdm.tripplanner.data.local.TripPlannerDatabase
import dam.pmdm.tripplanner.data.local.entity.ViajeEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueba de integración entre Room Database y ViajeDao.
 * Verifica que la inserción y consulta de viajes funciona correctamente
 * usando una base de datos en memoria (no persiste entre pruebas).
 */
@RunWith(AndroidJUnit4::class)
class TripPlannerIntegrationTest {

    private lateinit var db: TripPlannerDatabase

    /**
     * Inicializa la base de datos en memoria antes de cada prueba.
     * Se usa inMemoryDatabaseBuilder para que los datos no persistan.
     */
    @Before
    fun crearBaseDeDatos() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TripPlannerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    /**
     * Cierra la base de datos después de cada prueba.
     */
    @After
    fun cerrarBaseDeDatos() {
        db.close()
    }

    /**
     * Prueba de integración 1: Insertar un viaje en Room y recuperarlo por id
     * debe devolver el mismo viaje.
     */
    @Test
    fun insertar_y_obtenerViajePorId_correcto() = runBlocking {
        val viaje = ViajeEntity(
            idViaje = "viaje_integracion_1",
            nombre = "Viaje de prueba",
            paisDestino = "España",
            fechaInicio = System.currentTimeMillis(),
            fechaFin = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L,
            presupuestoTotal = 500.0,
            estado = "PLANIFICADO",
            idPropietario = "usuario_test"
        )

        // Insertar en Room
        db.viajeDao().insertar(viaje)

        // Recuperar de Room
        val resultado = db.viajeDao().obtenerPorId("viaje_integracion_1")

        // Verificar que el viaje recuperado es el mismo
        assertNotNull(resultado)
        assertEquals("viaje_integracion_1", resultado?.idViaje)
        assertEquals("Viaje de prueba", resultado?.nombre)
        assertEquals("España", resultado?.paisDestino)
        assertEquals(500.0, resultado?.presupuestoTotal ?: 0.0, 0.01)
        assertEquals("PLANIFICADO", resultado?.estado)
    }
}