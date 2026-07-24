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
        val excludedIds = CalendarPrefs.getExcludedEventIds(context)
        var scheduledCount = 0
        val newIds = mutableSetOf<String>()

        for (event in events) {
            if (event.id.toString() in excludedIds) continue

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

    // reschedules just one event, e.g. when the user flips it back on from the
    // events list, without touching the alarms already set for everything else
    fun scheduleSingleAlarm(context: Context, event: EventEntry): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val leadTimeMillis = CalendarPrefs.getLeadTimeMinutes(context) * 60 * 1000L
        val triggerAt = event.beginTime - leadTimeMillis
        if (triggerAt <= System.currentTimeMillis()) return false

        scheduleAlarm(context, alarmManager, event, triggerAt)

        val ids = CalendarPrefs.getScheduledEventIds(context).toMutableSet()
        ids.add(event.id.toString())
        CalendarPrefs.setScheduledEventIds(context, ids)

        return true
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
