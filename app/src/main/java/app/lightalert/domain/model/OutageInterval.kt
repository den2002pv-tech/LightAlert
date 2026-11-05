package app.lightalert.domain.model

import java.time.LocalTime

/**
 * A single daily interval during which electricity is expected to be unavailable.
 *
 * An interval may cross midnight. The interpretation of its date and time zone
 * belongs to the schedule that contains it.
 */
data class OutageInterval(
    val start: LocalTime,
    val end: LocalTime,
)
