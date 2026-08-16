package com.ame38.autocalendaralarms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationHelper {

    // channel settings are locked in once created on a device, so this id
    // was bumped when the channel switched to silent - AlarmSoundService now
    // owns the actual alarm tone (on the alarm stream) and vibration pattern,
    // and a leftover default-sound channel from an older install would just
    // layer a second, quieter ping underneath that
    const val CHANNEL_ID = "event_alarms_v2"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Event alarms",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts shown before your calendar events start"
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }
}
