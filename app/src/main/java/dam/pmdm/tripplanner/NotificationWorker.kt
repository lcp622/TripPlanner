package dam.pmdm.tripplanner

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import dam.pmdm.tripplanner.data.local.TripPlannerDatabase


class NotificacionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        FirebaseAuth.getInstance().currentUser ?: return Result.success()
        val db = TripPlannerDatabase.getInstance(applicationContext)

        val ahora = System.currentTimeMillis()
        val manana = ahora + 24 * 60 * 60 * 1000

        val actividades = db.actividadDao().obtenerTodasLasActividades()
            .filter { it.fecha in ahora..manana }

        if (actividades.isNotEmpty()) {
            crearNotificacion(
                mensaje = "Tienes ${actividades.size} actividad(es) en las próximas 24 horas"
            )
        }

        return Result.success()
    }

    private fun crearNotificacion(mensaje: String) {
        val notificationManager = applicationContext.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        val channel = NotificationChannel(
            "tripplanner_channel",
            "TripPlanner",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

        val notificacion = NotificationCompat.Builder(applicationContext, "tripplanner_channel")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("📅 Actividades próximas")
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notificacion)
    }
}