package com.ame38.autocalendaralarms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

// single source of truth for "what should currently have an alarm": the
// calendars and colors picked on the main screen. Used by the main screen
// itself (whenever a category is toggled), the events screen refresh, the
// periodic background worker and boot receiver, so all four stay consistent.
object EventSync {

    fun resync(context: Context): Int {
        val selectedIds = CalendarPrefs.getSelectedIds(context)
        if (selectedIds.isEmpty()) {
            return AlarmScheduler.scheduleAlarms(context, emptyList())
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return 0
        }

        val events = includedEvents(context, selectedIds)
        return AlarmScheduler.scheduleAlarms(context, events)
    }

    // the events that survive the main screen's sub-calendar + color picks,
    // i.e. the ones that should have an alarm unless individually excluded
    fun includedEvents(context: Context, selectedIds: Set<String>): List<EventEntry> {
        val allEvents = EventsRepository.queryUpcomingEvents(context, selectedIds)
        return allEvents.filter { !CalendarPrefs.isAccountColorExcluded(context, it.accountName, it.color) }
    }
}
