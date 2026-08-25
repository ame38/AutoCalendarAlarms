package com.ame38.autocalendaralarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

object AlarmScheduler {

    fun scheduleAlarms(context: Context, events: List<EventEntry>): Int {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val leadTimeMillis = CalendarPrefs.getLeadTimeMinutes(context) * 60 * 1000L
        val excludedIds = CalendarPrefs.getExcludedEventIds(context)
        var scheduledCount = 0
        val newKeys = mutableSetOf<String>()

        for (event in events) {
            if (event.id.toString() in excludedIds) continue

            val triggerAt = event.beginTime - leadTimeMillis
            if (triggerAt <= System.currentTimeMillis()) continue

            scheduleAlarm(context, alarmManager, event, triggerAt)
            newKeys.add(instanceKey(event))
            scheduledCount++
        }

        val staleKeys = CalendarPrefs.getScheduledEventIds(context) - newKeys
        for (staleKey in staleKeys) {
            cancelByKey(context, staleKey)
        }
        CalendarPrefs.setScheduledEventIds(context, newKeys)

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

        val keys = CalendarPrefs.getScheduledEventIds(context).toMutableSet()
        keys.add(instanceKey(event))
        CalendarPrefs.setScheduledEventIds(context, keys)

        return true
    }

    // exclusions are per event, not per occurrence, so this has to drop every
    // occurrence of a recurring event that's currently scheduled
    fun cancelAlarm(context: Context, eventId: Long) {
        val remaining = mutableSetOf<String>()

        for (key in CalendarPrefs.getScheduledEventIds(context)) {
            if (eventIdOf(key) == eventId) {
                cancelByKey(context, key)
            } else {
                remaining.add(key)
            }
        }

        CalendarPrefs.setScheduledEventIds(context, remaining)
    }

    // every occurrence of a recurring event shares one EVENT_ID, so the id on
    // its own can't identify an alarm - keying on the occurrence's start time
    // as well stops each occurrence from overwriting the previous one's
    // PendingIntent (which left only the furthest-out occurrence scheduled)
    private fun instanceKey(event: EventEntry): String = "${event.id}:${event.beginTime}"

    private fun eventIdOf(key: String): Long? = key.substringBefore(':').toLongOrNull()

    private fun cancelByKey(context: Context, key: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // keys stored before the per-occurrence change are a bare event id, and
        // their PendingIntent was built the old way - match it so alarms left
        // over from the previous version can still be cancelled
        val isLegacyKey = !key.contains(':')
        val requestCode = if (isLegacyKey) {
            (key.toLongOrNull() ?: return).toInt()
        } else {
            key.hashCode()
        }
        val intent = if (isLegacyKey) {
            Intent(context, AlarmReceiver::class.java)
        } else {
            alarmIntent(context, key)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    // the data uri is what actually makes two occurrences' intents distinct as
    // far as PendingIntent matching is concerned - extras are ignored for that,
    // so it can't rely on the event id/title it carries
    private fun alarmIntent(context: Context, key: String): Intent =
        Intent(context, AlarmReceiver::class.java).apply {
            data = Uri.parse("autocalendaralarms://alarm/$key")
        }

    private fun scheduleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        event: EventEntry,
        triggerAt: Long
    ) {
        val key = instanceKey(event)
        val intent = alarmIntent(context, key).apply {
            putExtra(AlarmReceiver.EXTRA_EVENT_ID, event.id)
            putExtra(AlarmReceiver.EXTRA_EVENT_TITLE, event.title)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            key.hashCode(),
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
