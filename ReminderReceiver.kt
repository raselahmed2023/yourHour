package com.example.yourhour

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Reminder"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        NotificationHelper.createNotificationChannel(context)

        val notification = NotificationCompat.Builder(context, "yourhour_limits")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("⏰ YourHour Reminder")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}