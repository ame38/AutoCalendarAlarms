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
            onChanged()
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
                Tab.SUB_CALENDARS -> buildSubCalendarChecklist(holder.checklistContainer, group, context)
                Tab.COLORS -> buildColorChecklist(holder.checklistContainer, group, context)
            }
        }
    }

    private fun styleTab(tab: TextView, selected: Boolean) {
        tab.setTypeface(null, if (selected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        tab.alpha = if (selected) 1f else 0.5f
    }

    private fun buildSubCalendarChecklist(container: LinearLayout, group: AccountGroup, context: android.content.Context) {
        for (calendar in group.calendars) {
            val row = CheckBox(context).apply {
                text = calendar.displayName
                isChecked = calendar.isChecked
                setOnCheckedChangeListener { _, isChecked ->
                    calendar.isChecked = isChecked
                    CalendarPrefs.setSelected(context, calendar.id, isChecked)
                    onChanged()
                }
            }
            container.addView(row)
        }
    }

    private fun buildColorChecklist(container: LinearLayout, group: AccountGroup, context: android.content.Context) {
        val distinctColors = group.calendars.map { it.color }.distinct()
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

            val checkBox = CheckBox(context).apply {
                text = context.getString(R.string.color_checkbox_label)
                isChecked = !CalendarPrefs.isAccountColorExcluded(context, group.accountName, color)
                setOnCheckedChangeListener { _, isChecked ->
                    CalendarPrefs.setAccountColorExcluded(context, group.accountName, color, !isChecked)
                    onChanged()
                }
            }

            row.addView(swatch)
            row.addView(checkBox)
            container.addView(row)
        }
    }

    override fun getItemCount(): Int = accounts.size
}
