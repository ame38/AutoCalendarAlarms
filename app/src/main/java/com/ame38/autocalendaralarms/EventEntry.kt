package com.ame38.autocalendaralarms

data class EventEntry(
    val id: Long,
    val title: String,
    val beginTime: Long,
    val calendarId: Long,
    val calendarDisplayName: String,
    val accountName: String,
    val color: Int
)
