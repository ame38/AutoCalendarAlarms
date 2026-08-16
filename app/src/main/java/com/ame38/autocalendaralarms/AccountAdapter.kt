package com.ame38.autocalendaralarms

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// each account (email) expands into exactly one of two checklists at a time -
// sub-calendars to include, or colors to exclude - never both, per account.
class AccountAdapter(
    private val accounts: List<AccountGroup>,
    private val eventCountsByCalendar: Map<Long, Int>,
    private val eventCountsByAccountColor: Map<Pair<String, Int>, Int>,
    private val colorsByAccount: Map<String, List<Int>>,
    private val onChanged: () -> Unit
) : RecyclerView.Adapter<AccountAdapter.AccountViewHolder>() {

    private enum class Tab { SUB_CALENDARS, COLORS }

    private val expandedAccounts = mutableSetOf<String>()
    private val selectedTab = mutableMapOf<String, Tab>()

    class AccountViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkBox: CheckBox = view.findViewById(R.id.accountCheckBox)
        val emailText: TextView = view.findViewById(R.id.accountEmailText)
        val expandButton: ImageButton = view.findViewById(R.id.accountExpandButton)
        val expandPanel: LinearLayout = view.findViewById(R.id.accountExpandPanel)
        val tabSubCalendars: TextView = view.findViewById(R.id.tabSubCalendars)
        val tabColors: TextView = view.findViewById(R.id.tabColors)
        val checklistContainer: LinearLayout = view.findViewById(R.id.accountChecklistContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_account, parent, false)
        return AccountViewHolder(view)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val group = accounts[position]
        val context = holder.itemView.context

        holder.emailText.text = group.accountName

        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = group.calendars.any { it.isChecked }
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            for (calendar in group.calendars) {
                calendar.isChecked = isChecked
                CalendarPrefs.setSelected(context, calendar.id, isChecked)
            }
            if (isChecked) {
                // re-enabling the whole account should restore every color too,
                // not leave it silently filtered by exclusions from before it was
                // turned off - matches the sub-calendar checkboxes above, which
                // all come back checked
                for (color in colorsByAccount[group.accountName].orEmpty()) {
                    CalendarPrefs.setAccountColorExcluded(context, group.accountName, color, false)
                }
            }
            onChanged()
            // the checklist below (if expanded) was built from the old isChecked
            // values and won't redraw on its own - force a rebind so it matches
            notifyItemChanged(holder.bindingAdapterPosition)
        }

        val isExpanded = group.accountName in expandedAccounts
        holder.expandPanel.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.expandButton.rotation = if (isExpanded) 180f else 0f
        holder.expandButton.setOnClickListener {
            if (isExpanded) {
                expandedAccounts.remove(group.accountName)
            } else {
                expandedAccounts.add(group.accountName)
            }
            notifyItemChanged(holder.bindingAdapterPosition)
        }

        val tab = selectedTab[group.accountName] ?: Tab.SUB_CALENDARS
        styleTab(holder.tabSubCalendars, tab == Tab.SUB_CALENDARS)
        styleTab(holder.tabColors, tab == Tab.COLORS)
        holder.tabSubCalendars.setOnClickListener {
            selectedTab[group.accountName] = Tab.SUB_CALENDARS
            notifyItemChanged(holder.bindingAdapterPosition)
        }
        holder.tabColors.setOnClickListener {
            selectedTab[group.accountName] = Tab.COLORS
            notifyItemChanged(holder.bindingAdapterPosition)
        }

        holder.checklistContainer.removeAllViews()
        if (isExpanded) {
            when (tab) {
                Tab.SUB_CALENDARS -> buildSubCalendarChecklist(holder, group)
                Tab.COLORS -> buildColorChecklist(holder, group)
            }
        }
    }

    private fun styleTab(tab: TextView, selected: Boolean) {
        tab.setTypeface(null, if (selected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        tab.alpha = if (selected) 1f else 0.5f
    }

    private fun buildSubCalendarChecklist(holder: AccountViewHolder, group: AccountGroup) {
        val context = holder.itemView.context
        for (calendar in group.calendars) {
            val count = eventCountsByCalendar[calendar.id] ?: 0
            val row = CheckBox(context).apply {
                text = "${calendar.displayName} ($count)"
                isChecked = calendar.isChecked
                setOnCheckedChangeListener { _, isChecked ->
                    calendar.isChecked = isChecked
                    CalendarPrefs.setSelected(context, calendar.id, isChecked)
                    onChanged()
                    // keeps the master checkbox above in sync with this change
                    notifyItemChanged(holder.bindingAdapterPosition)
                }
            }
            holder.checklistContainer.addView(row)
        }
    }

    private fun buildColorChecklist(holder: AccountViewHolder, group: AccountGroup) {
        val context = holder.itemView.context
        val container = holder.checklistContainer
        // the colors actually used by this account's upcoming events (including
        // per-event overrides), not just each calendar's base color - this is
        // the same set the events screen filters on, and the only set that lets
        // every event actually be excluded via a checkbox here
        val distinctColors = colorsByAccount[group.accountName].orEmpty()
        val swatchSize = (16 * context.resources.displayMetrics.density).toInt()

        for (color in distinctColors) {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, (4 * context.resources.displayMetrics.density).toInt(), 0, (4 * context.resources.displayMetrics.density).toInt())
            }

            val swatch = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(swatchSize, swatchSize).apply {
                    marginEnd = (8 * context.resources.displayMetrics.density).toInt()
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                }
            }

            val count = eventCountsByAccountColor[group.accountName to color] ?: 0
            val checkBox = CheckBox(context).apply {
                text = "${googleColorName(color)} ($count)"
                isChecked = !CalendarPrefs.isAccountColorExcluded(context, group.accountName, color)
                setOnCheckedChangeListener { _, isChecked ->
                    CalendarPrefs.setAccountColorExcluded(context, group.accountName, color, !isChecked)
                    onChanged()
                    notifyItemChanged(holder.bindingAdapterPosition)
                }
            }

            row.addView(swatch)
            row.addView(checkBox)
            container.addView(row)
        }
    }

    override fun getItemCount(): Int = accounts.size
}
