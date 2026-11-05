package app.lightalert.domain.parser

import app.lightalert.domain.model.ParsedOutageInterval
import app.lightalert.domain.model.ParsedQueue
import app.lightalert.domain.model.ParsedScheduleMessage
import java.time.DateTimeException
import java.time.LocalDate

/**
 * Parses Ukrainian hourly-outage announcements.
 *
 * The caller supplies the year because Telegram message text commonly omits
 * it. The parser deliberately returns only queues present in the message;
 * merging partial revisions is the responsibility of another component.
 */
class ScheduleMessageParser {

    fun parse(message: String, year: Int): ScheduleMessageParseResult {
        val dateMatch = DATE_REGEX.find(message)
        if (dateMatch == null) {
            return ScheduleMessageParseResult(
                schedule = null,
                warnings = listOf("Не удалось определить дату расписания."),
            )
        }

        val day = dateMatch.groupValues[1].toInt()
        val month = MONTHS[dateMatch.groupValues[2].lowercase()]
        val date = try {
            LocalDate.of(year, month ?: 0, day)
        } catch (_: DateTimeException) {
            null
        }

        if (date == null) {
            return ScheduleMessageParseResult(
                schedule = null,
                warnings = listOf("Дата расписания некорректна."),
            )
        }

        val queueMatches = QUEUE_REGEX.findAll(message).toList()
        if (queueMatches.isEmpty()) {
            return ScheduleMessageParseResult(
                schedule = null,
                warnings = listOf("Дата найдена, но очереди отключений не обнаружены."),
            )
        }

        val warnings = mutableListOf<String>()
        val queues = queueMatches.mapIndexed { index, match ->
            val sectionStart = match.range.last + 1
            val sectionEnd = queueMatches.getOrNull(index + 1)?.range?.first ?: message.length
            val section = message.substring(sectionStart, sectionEnd)
            val queueNumber = match.groupValues[1]
            val intervals = TIME_RANGE_REGEX.findAll(section).mapNotNull { timeMatch ->
                val start = parseTime(timeMatch.groupValues[1])
                val end = parseTime(timeMatch.groupValues[2])
                if (start == null || end == null || start >= end) {
                    warnings += "Некорректный интервал в очереди $queueNumber: ${timeMatch.value}"
                    null
                } else {
                    ParsedOutageInterval(startMinute = start, endMinute = end)
                }
            }.toList()

            if (intervals.isEmpty()) {
                warnings += "Для очереди $queueNumber интервалы не распознаны."
            }
            ParsedQueue(number = queueNumber, intervals = intervals)
        }

        val duplicateQueues = queues.groupBy { it.number }.filterValues { it.size > 1 }.keys
        duplicateQueues.forEach { warnings += "Очередь $it встречается в сообщении несколько раз." }

        return ScheduleMessageParseResult(
            schedule = ParsedScheduleMessage(date = date, queues = queues),
            warnings = warnings,
        )
    }

    private fun parseTime(value: String): Int? {
        val parts = value.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (minute !in 0..59) return null
        if (hour == 24 && minute == 0) return ParsedOutageInterval.MINUTES_PER_DAY
        if (hour !in 0..23) return null
        return hour * 60 + minute
    }

    private companion object {
        val DATE_REGEX = Regex(
            """(?iu)(?<!\d)(\d{1,2})\s+(січня|лютого|березня|квітня|травня|червня|липня|серпня|вересня|жовтня|листопада|грудня)"""
        )
        val QUEUE_REGEX = Regex(
            """(?imu)^\s*(?:🟢\s*)?Черга\s+(\d+\.\d+)\s*$"""
        )
        val TIME_RANGE_REGEX = Regex(
            """(?<!\d)(\d{1,2}:\d{2})\s*[–—-]\s*(\d{1,2}:\d{2})(?!\d)"""
        )
        val MONTHS = mapOf(
            "січня" to 1,
            "лютого" to 2,
            "березня" to 3,
            "квітня" to 4,
            "травня" to 5,
            "червня" to 6,
            "липня" to 7,
            "серпня" to 8,
            "вересня" to 9,
            "жовтня" to 10,
            "листопада" to 11,
            "грудня" to 12,
        )
    }
}

data class ScheduleMessageParseResult(
    val schedule: ParsedScheduleMessage?,
    val warnings: List<String>,
) {
    val isSuccessful: Boolean
        get() = schedule != null
}
