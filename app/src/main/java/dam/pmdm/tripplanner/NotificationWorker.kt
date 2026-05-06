package dam.pmdm.tripplanner

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import dam.pmdm.tripplanner.data.local.TripPlannerDatabase
import java.util.Calendar

class NotificacionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doWork(): Result {
        val user = FirebaseAuth.getInstance().currentUser ?: return Result.success()
        val db = TripPlannerDatabase.getInstance(applicationContext)

        val ahora = System.currentTimeMillis()
        val manana = ahora + 24 * 60 * 60 * 1000

        // Obtener actividades de las próximas 24 horas
        val actividades = db.actividadDao().obtenerTodasLasActividades()
            .filter { it.fecha in ahora..manana }

        if (actividades.isNotEmpty()) {
            crearNotificacion(
                titulo = "📅 Actividades próximas",
                mensaje = "Tienes ${actividades.size} actividad(es) en las próximas 24 horas"
            )
        }

        return Result.success()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun crearNotificacion(titulo: String, mensaje: String) {
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
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notificacion)
    }
}