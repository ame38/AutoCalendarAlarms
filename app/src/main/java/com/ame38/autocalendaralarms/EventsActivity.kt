package com.ame38.autocalendaralarms

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class EventsActivity : AppCompatActivity() {

    private lateinit var eventsList: RecyclerView
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_events)

        eventsList = findViewById(R.id.eventsList)
        eventsList.layoutManager = LinearLayoutManager(this)
        emptyText = findViewById(R.id.eventsEmptyText)

        findViewById<Button>(R.id.refreshButton).setOnClickListener {
            loadEvents()
        }

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

        val events = EventsRepository.queryUpcomingEvents(this, selectedIds)
        if (events.isEmpty()) {
            showEmpty(getString(R.string.no_upcoming_events))
        } else {
            emptyText.visibility = View.GONE
            eventsList.visibility = View.VISIBLE
            eventsList.adapter = EventAdapter(events)
            scheduleAlarms(events)
        }
    }

    private fun scheduleAlarms(events: List<EventEntry>) {
        val count = AlarmScheduler.scheduleAlarms(this, events)
        Toast.makeText(this, "$count alarms set", Toast.LENGTH_SHORT).show()
    }

    private fun showEmpty(message: String) {
        emptyText.text = message
        emptyText.visibility = View.VISIBLE
        eventsList.visibility = View.GONE
    }
}
