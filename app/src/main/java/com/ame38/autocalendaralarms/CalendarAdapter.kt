package com.ame38.autocalendaralarms

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CalendarAdapter(private val calendars: List<CalendarEntry>) :
    RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    class CalendarViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkBox: CheckBox = view.findViewById(R.id.calendarCheckBox)
        val nameText: TextView = view.findViewById(R.id.calendarNameText)
        val accountText: TextView = view.findViewById(R.id.calendarAccountText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar, parent, false)
        return CalendarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        val calendar = calendars[position]
        holder.nameText.text = calendar.displayName
        holder.accountText.text = calendar.accountName

        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = calendar.isChecked
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            calendar.isChecked = isChecked
        }
    }

    override fun getItemCount(): Int = calendars.size
}
