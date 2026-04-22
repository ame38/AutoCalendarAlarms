package com.ame38.autocalendaralarms

import android.content.Context

private const val PREFS_NAME = "calendar_prefs"
private const val KEY_SELECTED_IDS = "selected_calendar_ids"

object CalendarPrefs {

    fun getSelectedIds(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_SELECTED_IDS, emptySet()) ?: emptySet()
    }

    fun setSelected(context: Context, calendarId: Long, isChecked: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_SELECTED_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        val idString = calendarId.toString()

        if (isChecked) {
            current.add(idString)
        } else {
            current.remove(idString)
        }

        prefs.edit().putStringSet(KEY_SELECTED_IDS, current).apply()
    }
}
