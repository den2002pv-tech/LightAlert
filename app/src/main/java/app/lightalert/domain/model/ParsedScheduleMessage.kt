package app.lightalert.domain.model

import java.time.LocalDate

/**
 * A schedule extracted from one source message.
 *
 * Only queues explicitly present in the message are included. Missing queues
 * must never be interpreted as having no outages.
 */
data class ParsedScheduleMessage(
    val date: LocalDate,
    val queues: List<ParsedQueue>,
)

data class ParsedQueue(
    val number: String,
    val intervals: List<ParsedOutageInterval>,
)

/**
 * Minute offsets from midnight. 1440 is used for 24:00 so that the end of a
 * day is not confused with 00:00 at its beginning.
 */
data class ParsedOutageInterval(
    val startMinute: Int,
    val endMinute: Int,
) {
    init {
        require(startMinute in 0 until MINUTES_PER_DAY)
        require(endMinute in 1..MINUTES_PER_DAY)
        require(startMinute < endMinute)
    }

    companion object {
        const val MINUTES_PER_DAY = 1440
    }
}
