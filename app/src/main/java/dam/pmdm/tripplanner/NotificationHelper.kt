package dam.pmdm.tripplanner

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

/**
 * Muestra una notificación local de alta prioridad para eventos colaborativos del viaje.
 * Se usa para notificar a los participantes de cambios en tiempo real como
 * nuevas actividades, gastos añadidos o nuevos participantes en el viaje.
 *
 * A diferencia de [NotificacionWorker] que usa un canal de importancia normal
 * y un id fijo, esta función usa un canal de **importancia alta** (aparece como
 * heads-up notification) y un id basado en el timestamp para que cada
 * notificación colaborativa sea independiente y no sobreescriba las anteriores.
 *
 * El canal se crea si no existe cada vez que se llama a la función — Android
 * ignora la llamada si el canal ya está registrado, por lo que es seguro llamarla
 * repetidamente sin duplicar canales.
 *
 * @param context Contexto de la aplicación para acceder al sistema de notificaciones
 * @param titulo Título de la notificación (por ejemplo "Nueva actividad añadida")
 * @param mensaje Cuerpo de la notificación con los detalles del evento colaborativo
 */
fun mostrarNotificacionColaborativa(context: Context, titulo: String, mensaje: String) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Crear el canal de alta prioridad para eventos colaborativos
    val channel = NotificationChannel(
        "tripplanner_colaborativa",
        "TripPlanner Colaborativo",
        NotificationManager.IMPORTANCE_HIGH
    )
    notificationManager.createNotificationChannel(channel)

    val notificacion = NotificationCompat.Builder(context, "tripplanner_colaborativa")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(titulo)
        .setContentText(mensaje)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        // La notificación se descarta automáticamente al pulsar sobre ella
        .setAutoCancel(true)
        .build()

    // Se usa el timestamp como id para que cada notificación sea única
    // y no sobreescriba las notificaciones colaborativas anteriores
    notificationManager.notify(System.currentTimeMillis().toInt(), notificacion)
}