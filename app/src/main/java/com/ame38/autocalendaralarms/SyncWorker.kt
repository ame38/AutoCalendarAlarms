package com.ame38.autocalendaralarms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

// runs on a periodic background schedule so alarms stay up to date even if
// the app hasn't been opened in a while, same query + filtering as the
// events screen just without anything on screen to update
class SyncWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val selectedIds = CalendarPrefs.getSelectedIds(applicationContext)
        if (selectedIds.isEmpty()) return Result.success()

        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_CALENDAR)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        val allEvents = EventsRepository.queryUpcomingEvents(applicationContext, selectedIds)
        val excludedColors = CalendarPrefs.getExcludedColors(applicationContext)
        val events = allEvents.filter { it.color !in excludedColors }

        AlarmScheduler.scheduleAlarms(applicationContext, events)

        return Result.success()
    }
}
