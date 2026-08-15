package com.ame38.autocalendaralarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object AlarmScheduler {

    fun scheduleAlarms(context: Context, events: List<EventEntry>): Int {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val leadTimeMillis = CalendarPrefs.getLeadTimeMinutes(context) * 60 * 1000L
        var scheduledCount = 0
        val newIds = mutableSetOf<String>()

        for (event in events) {
            val triggerAt = event.beginTime - leadTimeMillis
            if (triggerAt <= System.currentTimeMillis()) continue

            scheduleAlarm(context, alarmManager, event, triggerAt)
            newIds.add(event.id.toString())
            scheduledCount++
        }

        val staleIds = CalendarPrefs.getScheduledEventIds(context) - newIds
        for (staleId in staleIds) {
            cancelAlarm(context, staleId.toLong())
        }
        CalendarPrefs.setScheduledEventIds(context, newIds)

        return scheduledCount
    }

    fun cancelAlarm(context: Context, eventId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
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
