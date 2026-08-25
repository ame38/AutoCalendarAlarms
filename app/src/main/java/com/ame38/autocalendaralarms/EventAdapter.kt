package com.ame38.autocalendaralarms

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventAdapter(private val events: List<EventEntry>) :
    RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    private val dateFormat = SimpleDateFormat("EEE, MMM d 'at' h:mm a", Locale.getDefault())

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val colorBar: View = view.findViewById(R.id.eventColorBar)
        val titleText: TextView = view.findViewById(R.id.eventTitleText)
        val timeText: TextView = view.findViewById(R.id.eventTimeText)
        val calendarText: TextView = view.findViewById(R.id.eventCalendarText)
        val alarmCheckBox: CheckBox = view.findViewById(R.id.eventAlarmCheckBox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        val context = holder.itemView.context
        holder.titleText.text = event.title
        holder.timeText.text = dateFormat.format(Date(event.beginTime))
        holder.calendarText.text = event.calendarDisplayName
        holder.calendarText.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 8 * context.resources.displayMetrics.density
            setColor(calendarTagColor(event.calendarId))
        }
        holder.colorBar.setBackgroundColor(event.color)

        val isExcluded = CalendarPrefs.getExcludedEventIds(context).contains(event.id.toString())

        holder.alarmCheckBox.setOnCheckedChangeListener(null)
        holder.alarmCheckBox.isChecked = !isExcluded
        holder.alarmCheckBox.setOnCheckedChangeListener { _, isChecked ->
            val excluded = !isChecked
            CalendarPrefs.setEventExcluded(context, event.id, excluded)
            if (excluded) {
                AlarmScheduler.cancelAlarm(context, event.id)
            } else {
                // a recurring event shows up once per occurrence but the
                // checkbox covers the whole event, so put every occurrence of
                // it back, not just the row that was tapped
                events.filter { it.id == event.id }
                    .forEach { AlarmScheduler.scheduleSingleAlarm(context, it) }
            }
        }
    }

    override fun getItemCount(): Int = events.size
}
