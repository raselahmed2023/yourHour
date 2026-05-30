package com.example.yourhour

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Phone reboot হলে VacationNoticeService আবার start করো
            val serviceIntent = Intent(context, VacationNoticeService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}