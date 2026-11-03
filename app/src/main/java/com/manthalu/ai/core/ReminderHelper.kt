package com.manthalu.ai.core

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.manthalu.ai.MainActivity
import com.manthalu.ai.R

/**
 * Simple local reminders for personal tasks.
 * Uses AlarmManager + notification channel.
 */
object ReminderHelper {
    const val CHANNEL_ID = "manthalu_reminders"
    const val ACTION_REMIND = "com.manthalu.ai.REMIND"
    const val EXTRA_TITLE = "title"
    const val EXTRA_ID = "id"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Manthalu Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Task and personal reminders" }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    fun schedule(
        context: Context,
        id: Int,
        title: String,
        triggerAtMillis: Long
    ) {
        ensureChannel(context)
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMIND
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_ID, id)
        }
        val pi = PendingIntent.getBroadcast(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }
        } catch (_: SecurityException) {
            // SCHEDULE_EXACT_ALARM may be required on newer Android; fall back
            am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    fun cancel(context: Context, id: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pi)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderHelper.ACTION_REMIND) return
        val title = intent.getStringExtra(ReminderHelper.EXTRA_TITLE) ?: "Reminder"
        val id = intent.getIntExtra(ReminderHelper.EXTRA_ID, 1)

        ReminderHelper.ensureChannel(context)
        val open = Intent(context, MainActivity::class.java)
        val contentPi = PendingIntent.getActivity(
            context, 0, open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, ReminderHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Manthalu")
            .setContentText(title)
            .setContentIntent(contentPi)
            .setAutoCancel(true)
            .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(id, notification)
    }
}
