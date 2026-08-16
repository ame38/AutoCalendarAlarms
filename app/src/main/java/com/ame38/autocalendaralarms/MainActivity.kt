package com.ame38.autocalendaralarms

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.CalendarContract
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
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

    // chained straight off the previous step's own callback rather than firing
    // all three requests back to back in onCreate - starting the exact-alarm or
    // battery settings screen while the notification dialog is still resolving
    // can cancel that dialog before the user's tap actually takes effect
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            requestExactAlarmPermissionIfNeeded()
        }

    private val requestExactAlarmSettingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            requestBatteryOptimizationExemptionIfNeeded()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        calendarList = findViewById(R.id.calendarList)
        calendarList.layoutManager = LinearLayoutManager(this)

        findViewById<Button>(R.id.viewEventsButton).setOnClickListener {
            startActivity(Intent(this, EventsActivity::class.java))
        }

        findViewById<ImageButton>(R.id.refreshButton).setOnClickListener {
            forceSync()
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            requestExactAlarmPermissionIfNeeded()
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            requestExactAlarmPermissionIfNeeded()
        }
    }

    // without this, alarms silently fall back to AlarmManager.set() which doze/app
    // standby can delay by a long, unpredictable margin - which is why alarms can
    // appear to "not go off". Asking up front avoids that fallback entirely.
    private fun requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            requestBatteryOptimizationExemptionIfNeeded()
            return
        }

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (alarmManager.canScheduleExactAlarms()) {
            requestBatteryOptimizationExemptionIfNeeded()
            return
        }

        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:$packageName")
        }
        requestExactAlarmSettingsLauncher.launch(intent)
    }

    // so the periodic sync doesn't get killed off by doze/app standby, only
    // asks once since isIgnoringBatteryOptimizations is already true after that
    private fun requestBatteryOptimizationExemptionIfNeeded() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (powerManager.isIgnoringBatteryOptimizations(packageName)) return

        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }

    private fun setupLeadTimeOptions() {
        val checkedId = when (CalendarPrefs.getLeadTimeMinutes(this)) {
            5 -> R.id.leadTime5
            10 -> R.id.leadTime10
            30 -> R.id.leadTime30
            60 -> R.id.leadTime60
            else -> R.id.leadTime15
        }
        leadTimeGroup.check(checkedId)

        leadTimeGroup.setOnCheckedChangeListener { _, checkedId ->
            val minutes = when (checkedId) {
                R.id.leadTime5 -> 5
                R.id.leadTime10 -> 10
                R.id.leadTime30 -> 30
                R.id.leadTime60 -> 60
                else -> 15
            }
            CalendarPrefs.setLeadTimeMinutes(this, minutes)
            forceSync()
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
            calendarList.adapter = AccountAdapter(calendars.groupByAccount()) { forceSync() }
        }
    }

    // sub-calendar and color choices here are what actually control which events
    // get an alarm, so every change needs to immediately re-run scheduling
    private fun forceSync() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val count = EventSync.resync(this)
        Toast.makeText(this, "$count alarms set", Toast.LENGTH_SHORT).show()
    }

    private fun queryCalendars(): List<CalendarEntry> {
        val calendars = mutableListOf<CalendarEntry>()
        val selectedIds = CalendarPrefs.getSelectedIds(this)
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR
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
            val colorIndex = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                calendars.add(
                    CalendarEntry(
                        id = id,
                        displayName = cursor.getString(nameIndex) ?: "",
                        accountName = cursor.getString(accountIndex) ?: "",
                        color = cursor.getInt(colorIndex),
                        isChecked = selectedIds.contains(id.toString())
                    )
                )
            }
        }

        return calendars
    }
}
