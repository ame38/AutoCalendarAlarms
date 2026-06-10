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
            CalendarContract.Instances.CALENDAR_ID
        )

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, now)
        ContentUris.appendId(builder, end)

        context.contentResolver.query(
            builder.build(),
            projection,
            null,
            null,
            "${CalendarContract.Instances.BEGIN} ASC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID)
            val titleIndex = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
            val beginIndex = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
            val calendarIdIndex = cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_ID)

            while (cursor.moveToNext()) {
                val calendarId = cursor.getLong(calendarIdIndex)
                if (!selectedIds.contains(calendarId.toString())) continue

                events.add(
                    EventEntry(
                        id = cursor.getLong(idIndex),
                        title = cursor.getString(titleIndex) ?: "",
                        beginTime = cursor.getLong(beginIndex),
                        calendarId = calendarId
                    )
                )
            }
        }

        return events
    }
}
