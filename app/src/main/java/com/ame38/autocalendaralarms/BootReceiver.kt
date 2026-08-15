package com.ame38.autocalendaralarms

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val selectedIds = CalendarPrefs.getSelectedIds(context)
        if (selectedIds.isEmpty()) return

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val events = EventsRepository.queryUpcomingEvents(context, selectedIds)
        AlarmScheduler.scheduleAlarms(context, events)
    }
}
