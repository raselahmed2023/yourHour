package com.example.yourhour

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

class VacationNoticeService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val CHANNEL_ID   = "vacation_notice"
    private val NOTIF_ID     = 999

    private var continuousActiveMinutes = 0
    private var lastResetDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        startForeground(NOTIF_ID, buildForegroundNotification())
        startMonitoring()
        return START_STICKY
    }

    private fun startMonitoring() {
        serviceScope.launch {
            while (true) {
                delay(60_000) // প্রতি ১ মিনিট

                // 1. Midnight reset — নতুন দিনে সব reset
                resetAtMidnightIfNeeded()

                // 2. Vacation notice — ৪৫ মিনিট continuous use
                if (wasPhoneActiveInLastMinute()) {
                    continuousActiveMinutes++
                    if (continuousActiveMinutes >= 45) {
                        sendVacationNotice()
                        continuousActiveMinutes = 0
                    }
                } else {
                    continuousActiveMinutes = 0
                }

                // 3. Daily goal alert — 50%, 80%, 100%
                checkDailyGoalAlert()
            }
        }
    }

    // ── Daily Goal Alert ─────────────────────────────────────────
    // GoalManager থেকে goal নেয়, আজকের total usage check করে
    private suspend fun checkDailyGoalAlert() {
        val goalManager = GoalManager(this)
        var goalHours = 4L // default

        // goal flow থেকে একটা value নেও
        try {
            withTimeoutOrNull(500) {
                goalManager.dailyGoalFlow.collect { goalHours = it; return@collect }
            }
        } catch (e: Exception) { return }

        if (goalHours <= 0) return

        val goalMillis  = goalHours * 3600_000L
        val usedMillis  = getTodayTotalUsage()
        val usedPercent = (usedMillis * 100 / goalMillis).toInt()

        val prefs = getSharedPreferences("goal_alert_prefs", Context.MODE_PRIVATE)

        when {
            usedPercent >= 100 && !prefs.getBoolean("alert_100", false) -> {
                sendGoalAlert(100, goalHours, usedMillis)
                prefs.edit().putBoolean("alert_100", true).apply()
            }
            usedPercent >= 80 && !prefs.getBoolean("alert_80", false) -> {
                sendGoalAlert(80, goalHours, usedMillis)
                prefs.edit().putBoolean("alert_80", true).apply()
            }
            usedPercent >= 50 && !prefs.getBoolean("alert_50", false) -> {
                sendGoalAlert(50, goalHours, usedMillis)
                prefs.edit().putBoolean("alert_50", true).apply()
            }
        }
    }

    // আজকের মোট screen time (midnight থেকে এখন পর্যন্ত)
    private fun getTodayTotalUsage(): Long {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val startCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val stats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startCal.timeInMillis,
            System.currentTimeMillis()
        )
        return stats.sumOf { it.totalTimeInForeground }
    }

    private fun sendGoalAlert(percent: Int, goalHours: Long, usedMillis: Long) {
        val usedHours = TimeUnit.MILLISECONDS.toHours(usedMillis)
        val usedMin   = TimeUnit.MILLISECONDS.toMinutes(usedMillis) % 60

        val (title, body) = when (percent) {
            50  -> Pair(
                "⚠️ 50% of your daily goal used",
                "You've used ${usedHours}h ${usedMin}m out of your ${goalHours}h goal. Stay mindful!"
            )
            80  -> Pair(
                "🔴 80% of your daily goal used!",
                "Only ${goalHours - usedHours}h left of your ${goalHours}h goal. Consider putting the phone down."
            )
            100 -> Pair(
                "🚫 Daily goal exceeded!",
                "You've used ${usedHours}h ${usedMin}m — past your ${goalHours}h goal. Time to rest."
            )
            else -> Pair("YourHour Alert", "Screen time alert")
        }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // unique ID যাতে তিনটা আলাদা notification দেখায়
        manager.notify(9000 + percent, notification)
    }

    // ── Midnight reset ───────────────────────────────────────────
    private fun resetAtMidnightIfNeeded() {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        if (today != lastResetDay) {
            // goal alerts reset
            getSharedPreferences("goal_alert_prefs", Context.MODE_PRIVATE)
                .edit().clear().apply()
            // vacation notice reset
            continuousActiveMinutes = 0
            lastResetDay = today
        }
    }

    // ── Screen active check ──────────────────────────────────────
    private fun wasPhoneActiveInLastMinute(): Boolean {
        val usm   = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now   = System.currentTimeMillis()
        val start = now - 65_000L
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, start, now)
        return stats.any { stat ->
            stat.lastTimeUsed >= start &&
                    stat.totalTimeInForeground > 0 &&
                    stat.packageName != packageName
        }
    }

    // ── Vacation notice ──────────────────────────────────────────
    private fun sendVacationNotice() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🌿 Take a 1 Minute Break!")
            .setContentText("You've been on your phone for 45 minutes. Rest your eyes.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
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
            .setContentText("Monitoring screen time — goal alerts & break reminders active")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "YourHour Alerts", NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Screen time goal alerts and break reminders" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}