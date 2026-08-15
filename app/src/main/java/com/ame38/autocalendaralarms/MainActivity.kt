package com.ame38.autocalendaralarms

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.view.View
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var calendarList: RecyclerView
    private lateinit var permissionText: TextView
    private lateinit var emptyText: TextView
    private lateinit var leadTimeGroup: RadioGroup

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                loadCalendars()
            } else {
                showPermissionDenied()
            }
        }

    // just so alarms can actually show a notification, we don't do anything special if denied
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        calendarList = findViewById(R.id.calendarList)
        calendarList.layoutManager = LinearLayoutManager(this)

        findViewById<Button>(R.id.viewEventsButton).setOnClickListener {
            startActivity(Intent(this, EventsActivity::class.java))
        }

        permissionText = findViewById(R.id.permissionText)
        permissionText.setOnClickListener {
            requestPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        }

        emptyText = findViewById(R.id.emptyText)

        leadTimeGroup = findViewById(R.id.leadTimeGroup)
        setupLeadTimeOptions()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR)
            == PackageManager.PERMISSION_GRANTED
        ) {
            loadCalendars()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        }

        requestNotificationPermissionIfNeeded()

        SyncScheduler.schedulePeriodicSync(this)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun setupLeadTimeOptions() {
        val checkedId = when (CalendarPrefs.getLeadTimeMinutes(this)) {
            5 -> R.id.leadTime5
            30 -> R.id.leadTime30
            60 -> R.id.leadTime60
            else -> R.id.leadTime15
        }
        leadTimeGroup.check(checkedId)

        leadTimeGroup.setOnCheckedChangeListener { _, checkedId ->
            val minutes = when (checkedId) {
                R.id.leadTime5 -> 5
                R.id.leadTime30 -> 30
                R.id.leadTime60 -> 60
                else -> 15
            }
            CalendarPrefs.setLeadTimeMinutes(this, minutes)
        }
    }

    private fun showPermissionDenied() {
        permissionText.visibility = View.VISIBLE
        emptyText.visibility = View.GONE
        calendarList.visibility = View.GONE
    }

    private fun loadCalendars() {
        val calendars = queryCalendars()
        permissionText.visibility = View.GONE

        if (calendars.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            calendarList.visibility = View.GONE
        } else {
            emptyText.visibility = View.GONE
            calendarList.visibility = View.VISIBLE
            calendarList.adapter = CalendarAdapter(calendars)
        }
    }

    private fun queryCalendars(): List<CalendarEntry> {
        val calendars = mutableListOf<CalendarEntry>()
        val selectedIds = CalendarPrefs.getSelectedIds(this)
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME
        )

        contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(CalendarContract.Calendars._ID)
            val nameIndex = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accountIndex = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                calendars.add(
                    CalendarEntry(
                        id = id,
                        displayName = cursor.getString(nameIndex) ?: "",
                        accountName = cursor.getString(accountIndex) ?: "",
                        isChecked = selectedIds.contains(id.toString())
                    )
                )
            }
        }

        return calendars
    }
}
