package com.ame38.autocalendaralarms

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import java.util.Calendar

object EventsRepository {

    fun queryUpcomingEvents(context: Context, selectedIds: Set<String>): List<EventEntry> {
        val events = mutableListOf<EventEntry>()

        val now = Calendar.getInstance().timeInMillis
        val end = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 30) }.timeInMillis

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.EVENT_COLOR,
            CalendarContract.Instances.CALENDAR_COLOR
        )

        if (selectedIds.isEmpty()) return events

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
                        color = color
                    )
                )
            }
        }

        return events
    }
}
