package app.lightalert.domain.merger

import app.lightalert.domain.model.EffectiveDailySchedule
import app.lightalert.domain.model.ParsedOutageInterval
import app.lightalert.domain.model.ParsedQueue
import app.lightalert.domain.model.ParsedScheduleMessage
import app.lightalert.domain.model.RevisionMergeStatus
import app.lightalert.domain.model.ScheduleRevision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ScheduleRevisionMergerTest {
    private val merger = ScheduleRevisionMerger()
    private val date = LocalDate.of(2026, 11, 5)
    private val firstTime = Instant.parse("2026-11-05T08:00:00Z")

    @Test
    fun initialRevisionCreatesEffectiveSchedule() {
        val result = merger.merge(null, revision("initial", firstTime, queue("1.1", 480, 600)))

        assertEquals(RevisionMergeStatus.APPLIED, result.status)
        assertEquals(listOf(480), result.schedule.queues["1.1"]!!.map { it.startMinute })
    }

    @Test
    fun partialRevisionReplacesOnlyQueuesPresentInMessage() {
        val initial = merger.merge(
            null,
            revision("full", firstTime, queue("1.1", 480, 600), queue("2.1", 600, 720)),
        ).schedule

        val result = merger.merge(
            initial,
            revision("partial", firstTime.plusSeconds(600), queue("1.1", 540, 660)),
        )

        assertEquals(RevisionMergeStatus.APPLIED, result.status)
        assertEquals(540, result.schedule.queues["1.1"]!!.single().startMinute)
        assertEquals(600, result.schedule.queues["2.1"]!!.single().startMinute)
    }

    @Test
    fun emptyQueueDataDoesNotErasePreviouslyKnownIntervals() {
        val initial = merger.merge(
            null,
            revision("valid", firstTime, queue("1.1", 480, 600)),
        ).schedule

        val result = merger.merge(
            initial,
            revision("empty", firstTime.plusSeconds(600), ParsedQueue("1.1", emptyList())),
        )

        assertEquals(RevisionMergeStatus.NO_VALID_QUEUE_DATA, result.status)
        assertEquals(480, result.schedule.queues["1.1"]!!.single().startMinute)
        assertTrue("Unapplied source must not be marked processed", "empty" !in result.schedule.processedSourceMessageIds)
    }

    @Test
    fun duplicateSourceMessageIsIdempotent() {
        val first = merger.merge(null, revision("same-id", firstTime, queue("1.1", 480, 600)))
        val second = merger.merge(first.schedule, revision("same-id", firstTime.plusSeconds(600), queue("1.1", 600, 720)))

        assertEquals(RevisionMergeStatus.DUPLICATE, second.status)
        assertEquals(480, second.schedule.queues["1.1"]!!.single().startMinute)
    }

    @Test
    fun olderRevisionCannotOverwriteNewerSchedule() {
        val newer = merger.merge(null, revision("newer", firstTime.plusSeconds(600), queue("1.1", 600, 720)))
        val older = merger.merge(newer.schedule, revision("older", firstTime, queue("1.1", 480, 540)))

        assertEquals(RevisionMergeStatus.STALE, older.status)
        assertEquals(600, older.schedule.queues["1.1"]!!.single().startMinute)
    }

    @Test
    fun revisionForAnotherDateIsRejected() {
        val current = EffectiveDailySchedule(date = date)
        val otherDate = ParsedScheduleMessage(date.plusDays(1), listOf(queue("1.1", 480, 600)))
        val result = merger.merge(current, ScheduleRevision("other-day", firstTime, otherDate))

        assertEquals(RevisionMergeStatus.DATE_MISMATCH, result.status)
        assertEquals(date, result.schedule.date)
    }

    private fun revision(id: String, at: Instant, vararg queues: ParsedQueue) =
        ScheduleRevision(id, at, ParsedScheduleMessage(date, queues.toList()))

    private fun queue(number: String, start: Int, end: Int) =
        ParsedQueue(number, listOf(ParsedOutageInterval(start, end)))
}
