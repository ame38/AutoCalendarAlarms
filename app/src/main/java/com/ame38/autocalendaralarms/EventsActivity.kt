package com.ame38.autocalendaralarms

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CalendarContract
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar

class EventsActivity : AppCompatActivity() {

    private lateinit var eventsList: RecyclerView
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_events)

        eventsList = findViewById(R.id.eventsList)
        eventsList.layoutManager = LinearLayoutManager(this)
        emptyText = findViewById(R.id.eventsEmptyText)

        loadEvents()
    }

    private fun loadEvents() {
        val selectedIds = CalendarPrefs.getSelectedIds(this)
        if (selectedIds.isEmpty()) {
            showEmpty(getString(R.string.no_calendars_selected))
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR)
            != PackageManager.PERMISSION_GRANTED
        ) {
            showEmpty(getString(R.string.calendar_permission_denied))
            return
        }

        val events = queryUpcomingEvents(selectedIds)
        if (events.isEmpty()) {
            showEmpty(getString(R.string.no_upcoming_events))
        } else {
            emptyText.visibility = View.GONE
            eventsList.visibility = View.VISIBLE
            eventsList.adapter = EventAdapter(events)
        }
    }

    private fun showEmpty(message: String) {
        emptyText.text = message
        emptyText.visibility = View.VISIBLE
        eventsList.visibility = View.GONE
    }

    private fun queryUpcomingEvents(selectedIds: Set<String>): List<EventEntry> {
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

        contentResolver.query(
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
