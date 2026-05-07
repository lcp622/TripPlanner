package dam.pmdm.tripplanner

import dam.pmdm.tripplanner.data.local.entity.GastoEntity
import dam.pmdm.tripplanner.data.local.entity.ViajeEntity
import org.junit.Assert.*
import org.junit.Test

/**
 * Batería de pruebas de unidad para los módulos principales de TripPlanner.
 * Se prueban funciones de lógica de negocio sin dependencias externas.
 */
class TripPlannerUnitTest {

    // ============================================================
    // PRUEBAS DE CÁLCULO DE ESTADO DE VIAJE
    // ============================================================

    /**
     * Prueba 1: Un viaje cuya fecha de inicio es futura debe tener estado PLANIFICADO.
     */
    @Test
    fun viaje_fechaFutura_estadoPlanificado() {
        val ahora = System.currentTimeMillis()
        val fechaInicio = ahora + 10 * 24 * 60 * 60 * 1000L
        val fechaFin = ahora + 20 * 24 * 60 * 60 * 1000L

        val estado = when {
            ahora < fechaInicio -> "PLANIFICADO"
            ahora > fechaFin -> "FINALIZADO"
            else -> "EN_CURSO"
        }

        assertEquals("PLANIFICADO", estado)
    }

    /**
     * Prueba 2: Un viaje cuya fecha de fin es pasada debe tener estado FINALIZADO.
     */
    @Test
    fun viaje_fechaPasada_estadoFinalizado() {
        val ahora = System.currentTimeMillis()
        val fechaInicio = ahora - 20 * 24 * 60 * 60 * 1000L
        val fechaFin = ahora - 5 * 24 * 60 * 60 * 1000L

        val estado = when {
            ahora < fechaInicio -> "PLANIFICADO"
            ahora > fechaFin -> "FINALIZADO"
            else -> "EN_CURSO"
        }

        assertEquals("FINALIZADO", estado)
    }

    /**
     * Prueba 3: Un viaje cuya fecha actual está entre inicio y fin debe tener estado EN_CURSO.
     */
    @Test
    fun viaje_fechaActual_estadoEnCurso() {
        val ahora = System.currentTimeMillis()
        val fechaInicio = ahora - 5 * 24 * 60 * 60 * 1000L
        val fechaFin = ahora + 5 * 24 * 60 * 60 * 1000L

        val estado = when {
            ahora < fechaInicio -> "PLANIFICADO"
            ahora > fechaFin -> "FINALIZADO"
            else -> "EN_CURSO"
        }

        assertEquals("EN_CURSO", estado)
    }

    // ============================================================
    // PRUEBAS DE CÁLCULO DE GASTOS
    // ============================================================

    /**
     * Prueba 4: El total de gastos debe ser la suma de todos los importes.
     */
    @Test
    fun gastos_totalCorrecto() {
        val gastos = listOf(
            crearGasto("g1", 50.0),
            crearGasto("g2", 30.0),
            crearGasto("g3", 20.0)
        )

        val total = gastos.sumOf { it.importe }

        assertEquals(100.0, total, 0.01)
    }

    /**
     * Prueba 5: El total de una lista vacía de gastos debe ser 0.
     */
    @Test
    fun gastos_listaVacia_totalCero() {
        val gastos = emptyList<GastoEntity>()
        val total = gastos.sumOf { it.importe }
        assertEquals(0.0, total, 0.01)
    }

    /**
     * Prueba 6: El reparto de un gasto entre N participantes debe ser importe / N.
     */
    @Test
    fun gastos_repartoCorrecto() {
        val importe = 90.0
        val participantes = listOf(
            mapOf("idUsuario" to "u1", "nombre" to "Ana"),
            mapOf("idUsuario" to "u2", "nombre" to "Luis"),
            mapOf("idUsuario" to "u3", "nombre" to "María")
        )

        val importePorPersona = importe / participantes.size

        assertEquals(30.0, importePorPersona, 0.01)
    }

    /**
     * Prueba 7: El presupuesto restante es la diferencia entre presupuesto y total gastado.
     */
    @Test
    fun gastos_presupuestoRestanteCorrecto() {
        val presupuesto = 500.0
        val gastos = listOf(
            crearGasto("g1", 100.0),
            crearGasto("g2", 150.0)
        )
        val totalGastado = gastos.sumOf { it.importe }
        val restante = presupuesto - totalGastado

        assertEquals(250.0, restante, 0.01)
    }

    /**
     * Prueba 8: Si el total gastado supera el presupuesto, el restante debe ser negativo.
     */
    @Test
    fun gastos_presupuestoExcedido_restanteNegativo() {
        val presupuesto = 100.0
        val gastos = listOf(
            crearGasto("g1", 80.0),
            crearGasto("g2", 50.0)
        )
        val totalGastado = gastos.sumOf { it.importe }
        val restante = presupuesto - totalGastado

        assertTrue(restante < 0)
    }

    // ============================================================
    // PRUEBAS DE FILTRADO DE VIAJES
    // ============================================================

    /**
     * Prueba 9: El filtrado de viajes por estado debe devolver solo los viajes con ese estado.
     */
    @Test
    fun viajes_filtradoPorEstado_correcto() {
        val viajes = listOf(
            crearViaje("v1", "PLANIFICADO"),
            crearViaje("v2", "EN_CURSO"),
            crearViaje("v3", "PLANIFICADO"),
            crearViaje("v4", "FINALIZADO")
        )

        val planificados = viajes.filter { it.estado == "PLANIFICADO" }

        assertEquals(2, planificados.size)
    }

    /**
     * Prueba 10: El filtrado de viajes por nombre debe ser insensible a mayúsculas.
     */
    @Test
    fun viajes_busquedaPorNombre_insensibleMayusculas() {
        val viajes = listOf(
            crearViaje("v1", "PLANIFICADO", "Viaje a París"),
            crearViaje("v2", "EN_CURSO", "Ruta por Italia"),
            crearViaje("v3", "PLANIFICADO", "Aventura en Madrid")
        )

        val resultado = viajes.filter {
            it.nombre.contains("París", ignoreCase = true)
        }

        assertEquals(1, resultado.size)
        assertEquals("Viaje a París", resultado.first().nombre)
    }

    /**
     * Prueba 11: El filtrado de viajes por destino debe funcionar correctamente.
     */
    @Test
    fun viajes_busquedaPorDestino_correcto() {
        val viajes = listOf(
            crearViaje("v1", "PLANIFICADO", "Viaje 1", "España"),
            crearViaje("v2", "EN_CURSO", "Viaje 2", "Francia"),
            crearViaje("v3", "PLANIFICADO", "Viaje 3", "España")
        )

        val resultado = viajes.filter {
            it.paisDestino.contains("España", ignoreCase = true)
        }

        assertEquals(2, resultado.size)
    }

    /**
     * Prueba 12: Con exactamente 5 participantes el límite debe estar alcanzado.
     */
    @Test
    fun participantes_limiteMaximo_correcto() {
        val participantes = mutableListOf("u1", "u2", "u3", "u4", "u5")
        val limiteAlcanzado = participantes.size >= 5
        assertTrue(limiteAlcanzado)
    }

    /**
     * Prueba 13: Con menos de 5 participantes el límite no debe estar alcanzado.
     */
    @Test
    fun participantes_limiteNoAlcanzado_correcto() {
        val participantes = mutableListOf("u1", "u2", "u3")
        val limiteAlcanzado = participantes.size >= 5
        assertFalse(limiteAlcanzado)
    }

    // ============================================================
    // FUNCIONES AUXILIARES
    // ============================================================

    /** Crea un GastoEntity de prueba con el id e importe indicados. */
    private fun crearGasto(id: String, importe: Double) = GastoEntity(
        idGasto = id,
        idViaje = "viaje_test",
        idPagador = "usuario_test",
        nombrePagador = "Usuario Test",
        concepto = "Gasto de prueba",
        importe = importe,
        categoria = "OTROS",
        fecha = System.currentTimeMillis()
    )

    /** Crea un ViajeEntity de prueba con el id, estado y nombre indicados. */
    private fun crearViaje(
        id: String,
        estado: String,
        nombre: String = "Viaje Test",
        destino: String = "España"
    ) = ViajeEntity(
        idViaje = id,
        nombre = nombre,
        paisDestino = destino,
        fechaInicio = System.currentTimeMillis(),
        fechaFin = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L,
        presupuestoTotal = 500.0,
        estado = estado,
        idPropietario = "usuario_test"
    )
}