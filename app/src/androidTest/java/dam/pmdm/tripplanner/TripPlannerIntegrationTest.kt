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
 * Prueba de integración entre [TripPlannerDatabase] y [dam.pmdm.tripplanner.data.local.dao.ViajeDao].
 * Verifica que la capa de persistencia local funciona correctamente de extremo a extremo:
 * desde la inserción de un viaje hasta su recuperación por id.
 *
 * Se usa una base de datos Room **en memoria** ([Room.inMemoryDatabaseBuilder]) para que:
 * - Los datos no persistan entre pruebas (cada prueba parte de un estado limpio)
 * - No se necesite limpiar la base de datos manualmente tras cada prueba
 * - Las pruebas sean más rápidas al no escribir en disco
 *
 * Se ejecuta con [AndroidJUnit4] porque Room necesita un contexto Android real,
 * por lo que estas pruebas se ejecutan en el dispositivo o emulador (androidTest).
 *
 */
@RunWith(AndroidJUnit4::class)
class TripPlannerIntegrationTest {

    /** Base de datos en memoria usada durante las pruebas */
    private lateinit var db: TripPlannerDatabase

    /**
     * Inicializa la base de datos en memoria antes de cada prueba.
     * La anotación [@Before] garantiza que se ejecuta antes de cada [@Test].
     */
    @Before
    fun crearBaseDeDatos() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TripPlannerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    /**
     * Cierra la base de datos después de cada prueba para liberar recursos.
     * La anotación [@After] garantiza que se ejecuta después de cada [@Test],
     * incluso si la prueba falla.
     */
    @After
    fun cerrarBaseDeDatos() {
        db.close()
    }

    /**
     * Prueba de integración 1: insertar un viaje en Room y recuperarlo por id
     * debe devolver exactamente el mismo viaje con todos sus campos intactos.
     *
     * Mecanismo: se usa [runBlocking] para ejecutar las operaciones suspendidas
     * del DAO de forma síncrona en el hilo de prueba.
     *
     * Pasos:
     * 1. Crear un [ViajeEntity] de prueba con datos conocidos
     * 2. Insertarlo en Room mediante [dam.pmdm.tripplanner.data.local.dao.ViajeDao.insertar]
     * 3. Recuperarlo por id mediante [dam.pmdm.tripplanner.data.local.dao.ViajeDao.obtenerPorId]
     * 4. Verificar que todos los campos coinciden con los valores originales
     *
     * Entradas: viaje con id "viaje_integracion_1", nombre "Viaje de prueba",
     * destino "España", presupuesto 500.0, estado "PLANIFICADO"
     * Salida esperada: el viaje recuperado tiene exactamente los mismos valores
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

        // Paso 1: insertar el viaje en la base de datos local
        db.viajeDao().insertar(viaje)

        // Paso 2: recuperar el viaje por su id único
        val resultado = db.viajeDao().obtenerPorId("viaje_integracion_1")

        // Paso 3: verificar que el viaje recuperado existe y tiene los datos correctos
        assertNotNull("El viaje recuperado no debe ser null", resultado)
        assertEquals("viaje_integracion_1", resultado?.idViaje)
        assertEquals("Viaje de prueba", resultado?.nombre)
        assertEquals("España", resultado?.paisDestino)
        assertEquals(500.0, resultado?.presupuestoTotal ?: 0.0, 0.01)
        assertEquals("PLANIFICADO", resultado?.estado)
    }
}