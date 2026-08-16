package com.ame38.autocalendaralarms

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageButton
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

    private lateinit var subCalendarFilterLabel: TextView
    private lateinit var subCalendarFilterScroll: HorizontalScrollView
    private lateinit var subCalendarFilterRow: LinearLayout

    private lateinit var colorFilterLabel: TextView
    private lateinit var colorFilterScroll: HorizontalScrollView
    private lateinit var colorFilterRow: LinearLayout

    // view-only: hides events from the list for quick lookup, never touches
    // which events actually have an alarm scheduled
    private val hiddenCalendarIds = mutableSetOf<Long>()
    private val hiddenColors = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_events)

        eventsList = findViewById(R.id.eventsList)
        eventsList.layoutManager = LinearLayoutManager(this)
        emptyText = findViewById(R.id.eventsEmptyText)

        subCalendarFilterLabel = findViewById(R.id.subCalendarFilterLabel)
        subCalendarFilterScroll = findViewById(R.id.subCalendarFilterScroll)
        subCalendarFilterRow = findViewById(R.id.subCalendarFilterRow)

        colorFilterLabel = findViewById(R.id.colorFilterLabel)
        colorFilterScroll = findViewById(R.id.colorFilterScroll)
        colorFilterRow = findViewById(R.id.colorFilterRow)

        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        findViewById<ImageButton>(R.id.refreshButton).setOnClickListener {
            val count = EventSync.resync(this)
            Toast.makeText(this, "$count alarms set", Toast.LENGTH_SHORT).show()
            loadEvents()
        }

        loadEvents()
    }

    // one chip per distinct sub-calendar among the loaded events, tap one to
    // hide/show its events in this list only
    private fun setupSubCalendarFilter(events: List<EventEntry>) {
        subCalendarFilterRow.removeAllViews()

        val distinctCalendars = events.map { it.calendarId to it.calendarDisplayName }.distinct()
        if (distinctCalendars.isEmpty()) {
            subCalendarFilterLabel.visibility = View.GONE
            subCalendarFilterScroll.visibility = View.GONE
            return
        }
        subCalendarFilterLabel.visibility = View.VISIBLE
        subCalendarFilterScroll.visibility = View.VISIBLE

        val chipPadding = (8 * resources.displayMetrics.density).toInt()
        val chipMargin = (8 * resources.displayMetrics.density).toInt()

        for ((calendarId, displayName) in distinctCalendars) {
            val chip = TextView(this).apply {
                text = displayName
                setTextColor(Color.WHITE)
                setPadding(chipPadding, chipPadding / 2, chipPadding, chipPadding / 2)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = chipPadding.toFloat()
                    setColor(calendarTagColor(calendarId))
                }
                alpha = if (calendarId in hiddenCalendarIds) 0.35f else 1f
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.marginEnd = chipMargin
            chip.layoutParams = params

            chip.setOnClickListener {
                if (calendarId in hiddenCalendarIds) {
                    hiddenCalendarIds.remove(calendarId)
                } else {
                    hiddenCalendarIds.add(calendarId)
                }
                renderEvents(EventSync.includedEvents(this, CalendarPrefs.getSelectedIds(this)))
            }

            subCalendarFilterRow.addView(chip)
        }
    }

    // one small swatch per distinct color actually used by the loaded events, tap
    // one to hide/show its events in this list only
    private fun setupColorFilter(events: List<EventEntry>) {
        colorFilterRow.removeAllViews()

        val distinctColors = events.map { it.color }.distinct()
        if (distinctColors.isEmpty()) {
            colorFilterLabel.visibility = View.GONE
            colorFilterScroll.visibility = View.GONE
            return
        }
        colorFilterLabel.visibility = View.VISIBLE
        colorFilterScroll.visibility = View.VISIBLE

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
            swatch.alpha = if (color in hiddenColors) 0.25f else 1f

            swatch.setOnClickListener {
                if (color in hiddenColors) {
                    hiddenColors.remove(color)
                } else {
                    hiddenColors.add(color)
                }
                renderEvents(EventSync.includedEvents(this, CalendarPrefs.getSelectedIds(this)))
            }

            colorFilterRow.addView(swatch)
        }
    }

    private fun loadEvents() {
        val selectedIds = CalendarPrefs.getSelectedIds(this)
        if (selectedIds.isEmpty()) {
            subCalendarFilterLabel.visibility = View.GONE
            subCalendarFilterScroll.visibility = View.GONE
            colorFilterLabel.visibility = View.GONE
            colorFilterScroll.visibility = View.GONE
            showEmpty(getString(R.string.no_calendars_selected))
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR)
            != PackageManager.PERMISSION_GRANTED
        ) {
            subCalendarFilterLabel.visibility = View.GONE
            subCalendarFilterScroll.visibility = View.GONE
            colorFilterLabel.visibility = View.GONE
            colorFilterScroll.visibility = View.GONE
            showEmpty(getString(R.string.calendar_permission_denied))
            return
        }

        // everything the main screen's sub-calendar and color picks allow through -
        // this whole set gets an alarm, independent of what's hidden below for lookup
        val includedEvents = EventSync.includedEvents(this, selectedIds)
        AlarmScheduler.scheduleAlarms(this, includedEvents)
        renderEvents(includedEvents)
    }

    private fun renderEvents(includedEvents: List<EventEntry>) {
        setupSubCalendarFilter(includedEvents)
        setupColorFilter(includedEvents)

        val visibleEvents = includedEvents.filter {
            it.calendarId !in hiddenCalendarIds && it.color !in hiddenColors
        }

        if (visibleEvents.isEmpty()) {
            showEmpty(getString(R.string.no_upcoming_events, EventsRepository.UPCOMING_WINDOW_DAYS))
        } else {
            emptyText.visibility = View.GONE
            eventsList.visibility = View.VISIBLE
            eventsList.adapter = EventAdapter(visibleEvents)
        }
    }

    private fun showEmpty(message: String) {
        emptyText.text = message
        emptyText.visibility = View.VISIBLE
        eventsList.visibility = View.GONE
    }
}
