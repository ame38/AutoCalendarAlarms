package com.ame38.autocalendaralarms

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CalendarContract
import android.view.View
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

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                loadCalendars()
            } else {
                showPermissionDenied()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        calendarList = findViewById(R.id.calendarList)
        calendarList.layoutManager = LinearLayoutManager(this)

        permissionText = findViewById(R.id.permissionText)
        permissionText.setOnClickListener {
            requestPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        }

        emptyText = findViewById(R.id.emptyText)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR)
            == PackageManager.PERMISSION_GRANTED
        ) {
            loadCalendars()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
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
