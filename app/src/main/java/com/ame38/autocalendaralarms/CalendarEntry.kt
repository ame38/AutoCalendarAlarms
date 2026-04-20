package com.ame38.autocalendaralarms

data class CalendarEntry(
    val id: Long,
    val displayName: String,
    val accountName: String,
    var isChecked: Boolean = false
)
