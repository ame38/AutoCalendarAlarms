package com.ame38.autocalendaralarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

private const val LEAD_TIME_MILLIS = 15 * 60 * 1000L

object AlarmScheduler {

    fun scheduleAlarms(context: Context, events: List<EventEntry>): Int {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        var scheduledCount = 0

        for (event in events) {
            val triggerAt = event.beginTime - LEAD_TIME_MILLIS
            if (triggerAt <= System.currentTimeMillis()) continue

            scheduleAlarm(context, alarmManager, event, triggerAt)
            scheduledCount++
        }

        return scheduledCount
    }

    private fun scheduleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        event: EventEntry,
        triggerAt: Long
    ) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_EVENT_ID, event.id)
            putExtra(AlarmReceiver.EXTRA_EVENT_TITLE, event.title)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()

        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }
}
