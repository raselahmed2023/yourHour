package com.example.yourhour

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class VacationNoticeService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val CHANNEL_ID = "vacation_notice"
    private val NOTIFICATION_ID = 999

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        startMonitoring()
        return START_STICKY
    }

    private fun startMonitoring() {
        serviceScope.launch {
            var continuousMinutes = 0
            while (true) {
                delay(60_000) // প্রতি 1 মিনিট
                continuousMinutes++
                if (continuousMinutes >= 45) {
                    sendVacationNotice()
                    continuousMinutes = 0
                }
            }
        }
    }

    private fun sendVacationNotice() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🌿 Take a 1 Minute Break!")
            .setContentText(
                "You've been using your phone for 45 minutes. " +
                        "Rest your eyes, stretch, breathe."
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "You've been on your phone for 45 minutes.\n\n" +
                                "👁️ Rest your eyes\n" +
                                "🧘 Take a deep breath\n" +
                                "🚶 Stand up and stretch\n\n" +
                                "\"You will never have this day again.\""
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("YourHour is watching over you 👁️")
            .setContentText("Monitoring screen time — 45 min break reminders active")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Vacation Notice",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "45 minute break reminders"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}