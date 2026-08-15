package com.ame38.autocalendaralarms

import android.content.Context

private const val PREFS_NAME = "calendar_prefs"
private const val KEY_SELECTED_IDS = "selected_calendar_ids"
private const val KEY_LEAD_TIME_MINUTES = "lead_time_minutes"
private const val DEFAULT_LEAD_TIME_MINUTES = 15
private const val KEY_SCHEDULED_EVENT_IDS = "scheduled_event_ids"
private const val KEY_EXCLUDED_EVENT_IDS = "excluded_event_ids"
private const val KEY_EXCLUDED_COLORS = "excluded_colors"

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

    fun getLeadTimeMinutes(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_LEAD_TIME_MINUTES, DEFAULT_LEAD_TIME_MINUTES)
    }

    fun setLeadTimeMinutes(context: Context, minutes: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_LEAD_TIME_MINUTES, minutes).apply()
    }

    fun getScheduledEventIds(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_SCHEDULED_EVENT_IDS, emptySet()) ?: emptySet()
    }

    fun setScheduledEventIds(context: Context, ids: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_SCHEDULED_EVENT_IDS, ids).apply()
    }

    fun getExcludedEventIds(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_EXCLUDED_EVENT_IDS, emptySet()) ?: emptySet()
    }

    fun setExcludedEventIds(context: Context, ids: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_EXCLUDED_EVENT_IDS, ids).apply()
    }

    fun setEventExcluded(context: Context, eventId: Long, excluded: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_EXCLUDED_EVENT_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        val idString = eventId.toString()

        if (excluded) {
            current.add(idString)
        } else {
            current.remove(idString)
        }

        prefs.edit().putStringSet(KEY_EXCLUDED_EVENT_IDS, current).apply()
    }

    // nothing excluded by default, color filtering is opt in on top of the
    // calendars the user already picked
    fun getExcludedColors(context: Context): Set<Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getStringSet(KEY_EXCLUDED_COLORS, emptySet()) ?: emptySet()
        return stored.mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun setColorExcluded(context: Context, color: Int, excluded: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_EXCLUDED_COLORS, emptySet())?.toMutableSet() ?: mutableSetOf()
        val colorString = color.toString()

        if (excluded) {
            current.add(colorString)
        } else {
            current.remove(colorString)
        }

        prefs.edit().putStringSet(KEY_EXCLUDED_COLORS, current).apply()
    }
}
