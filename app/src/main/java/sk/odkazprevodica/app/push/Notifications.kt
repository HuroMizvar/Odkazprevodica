package sk.odkazprevodica.app.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import sk.odkazprevodica.app.MainActivity
import sk.odkazprevodica.app.R

/**
 * Notifikácia s veľkým písmom – čitateľná aj na držiaku v aute.
 * Vlastný vzhľad (RemoteViews): titulok 18 sp, text odkazu 24 sp tučne.
 */
object Notifications {

    const val CHANNEL_ID = "odkazy"
    const val EXTRA_PLATE = "open_plate"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel),
            NotificationManager.IMPORTANCE_HIGH, // zobrazí sa navrchu obrazovky so zvukom
        ).apply {
            description = context.getString(R.string.notif_channel_desc)
            enableVibration(true)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun show(context: Context, id: Int, title: String, body: String, plate: String?, urgent: Boolean) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        createChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (plate != null) putExtra(EXTRA_PLATE, plate)
        }
        val pending = PendingIntent.getActivity(
            context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val small = RemoteViews(context.packageName, R.layout.notification_small).apply {
            setTextViewText(R.id.notif_title, title)
            setTextViewText(R.id.notif_body, body)
        }
        val big = RemoteViews(context.packageName, R.layout.notification_big).apply {
            setTextViewText(R.id.notif_title, title)
            setTextViewText(R.id.notif_body, body)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFF5B301.toInt())
            .setContentTitle(title) // pre zamknutú obrazovku a hodinky
            .setContentText(body)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(small)
            .setCustomBigContentView(big)
            .setCustomHeadsUpContentView(big)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (urgent) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }
}
