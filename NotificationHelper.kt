package com.example.yourhour

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {

    private const val CHANNEL_ID   = "yourhour_limits"
    private const val CHANNEL_NAME = "App Limits"
    private const val PREF_NAME    = "notif_shown_prefs"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "YourHour app limit alerts" }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    fun sendLimitAlert(context: Context, appName: String, percent: Int) {
        // FIX: already shown check — একই app এর একই % দুইবার দেখাবে না
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val key   = "${appName}_$percent"
        if (prefs.getBoolean(key, false)) return  // আগেই দেখানো হয়েছে

        val message = when (percent) {
            50  -> "⚠️ You've used $appName for 50% of your limit!"
            80  -> "🔴 Warning! 80% of your $appName limit reached!"
            100 -> "🚫 Limit reached! Stop using $appName now!"
            else -> "Alert for $appName"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("YourHour Alert")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // FIX: unique ID = appName hash + percent, যাতে প্রতিটা app আলাদা notification পায়
        val notifId = (appName.hashCode() and 0xFFFF) * 1000 + percent
        context.getSystemService(NotificationManager::class.java)
            .notify(notifId, notification)

        // Mark as shown
        prefs.edit().putBoolean(key, true).apply()
    }

    // দিনের শুরুতে (midnight এ) reset করতে হবে যাতে পরদিন আবার alert আসে
    fun resetDailyAlerts(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }
}