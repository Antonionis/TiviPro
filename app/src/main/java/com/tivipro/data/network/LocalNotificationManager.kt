package com.tivipro.data.network

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.tivipro.MainActivity
import com.tivipro.R
import com.tivipro.model.SportsEvent

class SportsNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("event_title") ?: "Evento Deportivo"
        val channel = intent.getStringExtra("event_channel") ?: ""
        val competition = intent.getStringExtra("event_competition") ?: "Deportes"
        val timeDisplay = intent.getStringExtra("event_time") ?: ""

        LocalNotificationManager.showNotification(
            context,
            title = title,
            channel = channel,
            competition = competition,
            timeDisplay = timeDisplay
        )
    }
}

object LocalNotificationManager {

    private const val CHANNEL_ID = "tivipro_sports_alerts"
    private const val CHANNEL_NAME = "Alertas Deportivas TiviPro"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Recordatorios 10 minutos antes de partidos y eventos deportivos"
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @Suppress("MissingPermission")
    fun showNotification(
        context: Context,
        title: String,
        channel: String,
        competition: String,
        timeDisplay: String
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val textContent = "🏆 $title\n📺 Canal: $channel • 🕒 $timeDisplay"

        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle("⚽ Evento Deportivo en 10 minutos")
                .setContentText(textContent)
                .setStyle(NotificationCompat.BigTextStyle().bigText(textContent))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), notification)
            }
        } catch (_: Exception) {}
    }

    @Suppress("MissingPermission", "ScheduleExactAlarm")
    fun scheduleLocalNotification(context: Context, event: SportsEvent) {
        createNotificationChannel(context)

        try {
            val rawTimestamp = event.startTimestamp
            if (rawTimestamp <= 0L) return
            val eventStartSec = if (rawTimestamp > 10_000_000_000L) rawTimestamp / 1000L else rawTimestamp
            val nowSec = System.currentTimeMillis() / 1000L

            val alertTimeSec = eventStartSec - 600L // 10 minutes before
            val delaySec = alertTimeSec - nowSec

            if (delaySec <= 0L) {
                // Event starts in <= 10 mins, trigger notification immediately!
                showNotification(
                    context,
                    title = event.title,
                    channel = event.channelName,
                    competition = event.competition.ifBlank { event.sportCategory },
                    timeDisplay = event.dateTimeDisplay
                )
                return
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, SportsNotificationReceiver::class.java).apply {
                putExtra("event_title", event.title)
                putExtra("event_channel", event.channelName)
                putExtra("event_competition", event.competition.ifBlank { event.sportCategory })
                putExtra("event_time", event.dateTimeDisplay)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                event.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerMs = System.currentTimeMillis() + (delaySec * 1000L)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelLocalNotification(context: Context, eventId: String) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, SportsNotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                eventId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        } catch (_: Exception) {}
    }
}
