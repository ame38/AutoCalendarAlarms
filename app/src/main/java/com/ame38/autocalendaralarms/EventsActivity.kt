package com.ame38.autocalendaralarms

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class EventsActivity : AppCompatActivity() {

    private lateinit var eventsList: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var colorFilterScroll: HorizontalScrollView
    private lateinit var colorFilterRow: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_events)

        eventsList = findViewById(R.id.eventsList)
        eventsList.layoutManager = LinearLayoutManager(this)
        emptyText = findViewById(R.id.eventsEmptyText)
        colorFilterScroll = findViewById(R.id.colorFilterScroll)
        colorFilterRow = findViewById(R.id.colorFilterRow)

        findViewById<Button>(R.id.refreshButton).setOnClickListener {
            loadEvents()
        }

        loadEvents()
    }

    // one small swatch per distinct color actually used by the loaded events, tap
    // one to toggle whether events with that color are excluded
    private fun setupColorFilter(events: List<EventEntry>) {
        colorFilterRow.removeAllViews()

        val distinctColors = events.map { it.color }.distinct()
        if (distinctColors.isEmpty()) {
            colorFilterScroll.visibility = View.GONE
            return
        }
        colorFilterScroll.visibility = View.VISIBLE

        val excludedColors = CalendarPrefs.getExcludedColors(this)
        val swatchSize = (28 * resources.displayMetrics.density).toInt()
        val swatchMargin = (8 * resources.displayMetrics.density).toInt()

        for (color in distinctColors) {
            val swatch = View(this)
            val params = LinearLayout.LayoutParams(swatchSize, swatchSize)
            params.marginEnd = swatchMargin
            swatch.layoutParams = params
            swatch.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
            }
            swatch.alpha = if (color in excludedColors) 0.25f else 1f

            swatch.setOnClickListener {
                val isExcluded = color in CalendarPrefs.getExcludedColors(this)
                CalendarPrefs.setColorExcluded(this, color, !isExcluded)
                loadEvents()
            }

            colorFilterRow.addView(swatch)
        }
    }

    private fun loadEvents() {
        val selectedIds = CalendarPrefs.getSelectedIds(this)
        if (selectedIds.isEmpty()) {
            colorFilterScroll.visibility = View.GONE
            showEmpty(getString(R.string.no_calendars_selected))
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR)
            != PackageManager.PERMISSION_GRANTED
        ) {
            colorFilterScroll.visibility = View.GONE
            showEmpty(getString(R.string.calendar_permission_denied))
            return
        }

        val allEvents = EventsRepository.queryUpcomingEvents(this, selectedIds)
        setupColorFilter(allEvents)

        val excludedColors = CalendarPrefs.getExcludedColors(this)
        val events = allEvents.filter { it.color !in excludedColors }

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
