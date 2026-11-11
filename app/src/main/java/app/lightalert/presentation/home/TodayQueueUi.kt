package app.lightalert.presentation.home

import app.lightalert.domain.model.ParsedOutageInterval

/**
 * A queue and its effective outage intervals for the current day.
 */
data class TodayQueueUi(
    val number: String,
    val intervals: List<ParsedOutageInterval>,
)
