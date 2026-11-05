package app.lightalert.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ScheduleMessageParserTest {
    private val parser = ScheduleMessageParser()

    @Test
    fun parsesFullDailyScheduleAndTwentyFourHourEnd() {
        val message = """
            ❗️ 5 листопада з 00:00 до 24:00 у Черкаській області діятимуть графіки погодинних відключень.
            🟢 Черга 1.1
            ⏰ 00:00–03:00
            ⏰ 07:00–10:00
            🟢 Черга 2.1
            ⏰ 02:00–04:00
            ⏰ 22:00–24:00
        """.trimIndent()

        val result = parser.parse(message, year = 2026)

        assertTrue(result.isSuccessful)
        assertNotNull(result.schedule)
        assertEquals(LocalDate.of(2026, 11, 5), result.schedule!!.date)
        assertEquals(listOf("1.1", "2.1"), result.schedule.queues.map { it.number })
        assertEquals(0, result.schedule.queues[0].intervals[0].startMinute)
        assertEquals(1440, result.schedule.queues[1].intervals.last().endMinute)
    }

    @Test
    fun parsesUpdatedPartialScheduleWithoutInventingMissingQueues() {
        val message = """
            ❗️ Оновлено графік погодинних відключень на 27 жовтня
            🟢 Черга 1.1
            ⏰ 18:00–20:00
            🟢 Черга 3.1
            ⏰ 14:00–16:00
            🟢 Черга 5.1
            ⏰ 16:00–18:00
        """.trimIndent()

        val result = parser.parse(message, year = 2026)

        assertEquals(listOf("1.1", "3.1", "5.1"), result.schedule!!.queues.map { it.number })
        assertFalse(result.schedule.queues.any { it.number == "2.1" })
        assertFalse(result.schedule.queues.any { it.number == "4.1" })
        assertFalse(result.schedule.queues.any { it.number == "6.1" })
    }

    @Test
    fun parsesMessageWithEmojiAndDifferentDashCharacters() {
        val message = """
            🔴 Оновлений графік на 25 жовтня
            🟢 Черга 1.1
            21:00-24:00
            🟢 Черга 2.1
            07:00—09:00
        """.trimIndent()

        val result = parser.parse(message, year = 2026)

        assertEquals(2, result.schedule!!.queues.size)
        assertEquals(1440, result.schedule.queues[0].intervals.single().endMinute)
        assertEquals(7 * 60, result.schedule.queues[1].intervals.single().startMinute)
    }

    @Test
    fun reportsMissingDate() {
        val result = parser.parse("🟢 Черга 1.1\n⏰ 08:00–10:00", year = 2026)

        assertNull(result.schedule)
        assertTrue(result.warnings.isNotEmpty())
    }

    @Test
    fun reportsInvalidTimeAndKeepsValidIntervals() {
        val message = """
            5 листопада
            🟢 Черга 1.1
            ⏰ 25:00–26:00
            ⏰ 08:00–10:00
        """.trimIndent()

        val result = parser.parse(message, year = 2026)

        assertEquals(1, result.schedule!!.queues.single().intervals.size)
        assertTrue(result.warnings.isNotEmpty())
    }

    @Test
    fun rejectsImpossibleCalendarDate() {
        val result = parser.parse("31 лютого\n🟢 Черга 1.1\n⏰ 08:00–10:00", year = 2026)

        assertNull(result.schedule)
    }
}
