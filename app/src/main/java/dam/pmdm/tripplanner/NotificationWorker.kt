package dam.pmdm.tripplanner

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import dam.pmdm.tripplanner.data.local.TripPlannerDatabase

/**
 * Worker que ejecuta en segundo plano la comprobación de actividades próximas
 * y lanza una notificación local si el usuario tiene actividades en las
 * próximas 24 horas.
 *
 * Se programa como tarea periódica diaria mediante [androidx.work.WorkManager]
 * en [MainActivity], usando [androidx.work.ExistingPeriodicWorkPolicy.KEEP]
 * para evitar programar múltiples instancias del mismo worker.
 *
 * Se extiende [CoroutineWorker] en lugar de [androidx.work.Worker] para poder
 * usar funciones suspendidas del DAO de Room sin bloquear el hilo principal.
 *
 * @param context Contexto de la aplicación
 * @param params Parámetros del worker proporcionados por WorkManager
 */
class NotificacionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    /**
     * Lógica principal del worker ejecutada en segundo plano.
     * Pasos realizados:
     * 1. Verifica que hay un usuario autenticado — si no, termina sin hacer nada
     * 2. Obtiene todas las actividades almacenadas localmente en Room
     * 3. Filtra las actividades que ocurren en las próximas 24 horas
     * 4. Si hay actividades próximas, lanza una notificación local
     *
     * Se usa Room en lugar de Firestore para evitar consumo de red innecesario
     * y garantizar que el worker funciona aunque no haya conexión a internet.
     *
     * @return [Result.success] siempre — los errores se gestionan silenciosamente
     * para que WorkManager no reintente el worker innecesariamente
     */
    override suspend fun doWork(): Result {
        // Si no hay usuario autenticado no tiene sentido comprobar actividades
        FirebaseAuth.getInstance().currentUser ?: return Result.success()

        val db = TripPlannerDatabase.getInstance(applicationContext)

        val ahora = System.currentTimeMillis()
        val manana = ahora + 24 * 60 * 60 * 1000

        // Filtrar las actividades que ocurren en el rango de las próximas 24 horas
        val actividades = db.actividadDao().obtenerTodasLasActividades()
            .filter { it.fecha in ahora..manana }

        // Solo lanzar notificación si hay actividades próximas
        if (actividades.isNotEmpty()) {
            crearNotificacion(
                mensaje = "Tienes ${actividades.size} actividad(es) en las próximas 24 horas"
            )
        }

        return Result.success()
    }

    /**
     * Crea y muestra una notificación local con el mensaje indicado.
     * Crea el canal de notificación si no existe — en Android 8+ (API 26)
     * las notificaciones requieren un canal previamente registrado.
     *
     * Se usa un id fijo para la notificación de forma que si se lanza
     * varias veces en el mismo día solo aparezca una notificación actualizada
     * en lugar de múltiples notificaciones acumuladas.
     *
     * @param mensaje Texto descriptivo con el número de actividades próximas
     */
    private fun crearNotificacion(mensaje: String) {
        val notificationManager = applicationContext.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        // Crear el canal de notificación si no existe
        val channel = NotificationChannel(
            "tripplanner_channel",
            "TripPlanner",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

        val notificacion = NotificationCompat.Builder(applicationContext, "tripplanner_channel")
            .setSmallIcon(R.mipmap.tripplanericon)
            .setContentTitle("📅 Actividades próximas")
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            // La notificación se descarta automáticamente al pulsar sobre ella
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notificacion)
    }
}