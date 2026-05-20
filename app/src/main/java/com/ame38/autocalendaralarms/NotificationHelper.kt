package com.ame38.autocalendaralarms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationHelper {

    const val CHANNEL_ID = "event_alarms"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Event alarms",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts shown before your calendar events start"
        }
        manager.createNotificationChannel(channel)
    }
}
