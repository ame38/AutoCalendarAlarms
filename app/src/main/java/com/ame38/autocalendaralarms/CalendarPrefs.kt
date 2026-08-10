package com.ame38.autocalendaralarms

import android.content.Context

private const val PREFS_NAME = "calendar_prefs"
private const val KEY_SELECTED_IDS = "selected_calendar_ids"
private const val KEY_LEAD_TIME_MINUTES = "lead_time_minutes"
private const val DEFAULT_LEAD_TIME_MINUTES = 15
private const val KEY_SCHEDULED_EVENT_IDS = "scheduled_event_ids"
private const val KEY_EXCLUDED_EVENT_IDS = "excluded_event_ids"
private const val KEY_EXCLUDED_ACCOUNT_COLORS = "excluded_account_colors"
private const val ACCOUNT_COLOR_SEPARATOR = "|"

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
    // calendars the user already picked. Scoped per account since the same
    // color int can mean something different in two different accounts.
    fun isAccountColorExcluded(context: Context, accountName: String, color: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getStringSet(KEY_EXCLUDED_ACCOUNT_COLORS, emptySet()) ?: emptySet()
        return "$accountName$ACCOUNT_COLOR_SEPARATOR$color" in stored
    }

    fun setAccountColorExcluded(context: Context, accountName: String, color: Int, excluded: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_EXCLUDED_ACCOUNT_COLORS, emptySet())?.toMutableSet() ?: mutableSetOf()
        val key = "$accountName$ACCOUNT_COLOR_SEPARATOR$color"

        if (excluded) {
            current.add(key)
        } else {
            current.remove(key)
        }

        prefs.edit().putStringSet(KEY_EXCLUDED_ACCOUNT_COLORS, current).apply()
    }
}
