package com.ame38.autocalendaralarms

data class CalendarEntry(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val color: Int,
    var isChecked: Boolean = false
)

data class AccountGroup(
    val accountName: String,
    val calendars: List<CalendarEntry>
)

fun List<CalendarEntry>.groupByAccount(): List<AccountGroup> =
    groupBy { it.accountName }
        .toSortedMap()
        .map { (accountName, calendars) -> AccountGroup(accountName, calendars) }
