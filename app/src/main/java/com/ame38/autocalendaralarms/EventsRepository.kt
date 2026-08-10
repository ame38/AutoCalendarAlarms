package com.ame38.autocalendaralarms

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import java.util.Calendar

object EventsRepository {

    fun queryUpcomingEvents(context: Context, selectedIds: Set<String>): List<EventEntry> {
        val events = mutableListOf<EventEntry>()
        if (selectedIds.isEmpty()) return events

        val accountNames = queryAccountNames(context)

        val now = Calendar.getInstance().timeInMillis
        val end = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 30) }.timeInMillis

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            CalendarContract.Instances.EVENT_COLOR,
            CalendarContract.Instances.CALENDAR_COLOR
        )

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, now)
        ContentUris.appendId(builder, end)

        val selection = "${CalendarContract.Instances.CALENDAR_ID} IN (${selectedIds.joinToString(",") { "?" }})"
        val selectionArgs = selectedIds.toTypedArray()

        context.contentResolver.query(
            builder.build(),
            projection,
            selection,
            selectionArgs,
            "${CalendarContract.Instances.BEGIN} ASC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID)
            val titleIndex = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
            val beginIndex = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
            val calendarIdIndex = cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_ID)
            val calendarNameIndex = cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)
            val eventColorIndex = cursor.getColumnIndex(CalendarContract.Instances.EVENT_COLOR)
            val calendarColorIndex = cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_COLOR)

            while (cursor.moveToNext()) {
                val calendarId = cursor.getLong(calendarIdIndex)

                // an event can override the calendar's color in google calendar, falls
                // back to the calendar color when it hasn't been given its own
                val eventColor = cursor.getInt(eventColorIndex)
                val calendarColor = cursor.getInt(calendarColorIndex)
                val color = if (eventColor != 0) eventColor else calendarColor

                events.add(
                    EventEntry(
                        id = cursor.getLong(idIndex),
                        title = cursor.getString(titleIndex) ?: "",
                        beginTime = cursor.getLong(beginIndex),
                        calendarId = calendarId,
                        calendarDisplayName = cursor.getString(calendarNameIndex) ?: "",
                        accountName = accountNames[calendarId] ?: "",
                        color = color
                    )
                )
            }
        }

        return events
    }

    // account_name isn't guaranteed to be joined into the Instances view (it's
    // only declared on the Calendars table), so look it up separately instead
    // of risking a missing-column crash reading it off Instances
    private fun queryAccountNames(context: Context): Map<Long, String> {
        val accountNames = mutableMapOf<Long, String>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_NAME
        )

        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(CalendarContract.Calendars._ID)
            val accountIndex = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)

            while (cursor.moveToNext()) {
                accountNames[cursor.getLong(idIndex)] = cursor.getString(accountIndex) ?: ""
            }
        }

        return accountNames
    }
}
