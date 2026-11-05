package app.lightalert.data.ingestion

import app.lightalert.data.local.db.EffectiveQueueEntity
import app.lightalert.data.local.db.ScheduleHistoryStore
import app.lightalert.data.local.db.ScheduleRevisionEntity
import app.lightalert.data.local.db.SourceMessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ScheduleMessageIngestionServiceTest {

    private val zone = ZoneId.of("Europe/Kyiv")
    private val date = LocalDate.of(2026, 10, 23)

    @Test
    fun partialUpdateKeepsQueuesMissingFromNewMessage() = runBlocking {
        val store = FakeScheduleHistoryStore()
        val service = ScheduleMessageIngestionService(
            historyStore = store,
            zoneId = zone,
            clock = { date.atStartOfDay(zone).toInstant().toEpochMilli() },
        )

        val first = service.ingest(
            message(
                id = 1L,
                sentAt = date.atTime(8, 0).atZone(zone).toInstant().toEpochMilli(),
                receivedAt = date.atTime(8, 1).atZone(zone).toInstant().toEpochMilli(),
                text = """
                    Оновлено графік погодинних відключень на 23 жовтня
                    Черга 1.1
                    10:00–12:00
                    Черга 2.1
                    14:00–16:00
                """.trimIndent(),
            ),
        )
        assertEquals(IngestionStatus.APPLIED, first.status)

        val second = service.ingest(
            message(
                id = 2L,
                sentAt = date.atTime(9, 0).atZone(zone).toInstant().toEpochMilli(),
                receivedAt = date.atTime(9, 1).atZone(zone).toInstant().toEpochMilli(),
                text = """
                    Оновлено графік погодинних відключень на 23 жовтня
                    Черга 1.1
                    11:00–13:00
                """.trimIndent(),
            ),
        )
        assertEquals(IngestionStatus.APPLIED, second.status)

        val queues = store.getEffectiveQueues(date.toString()).associateBy { it.queueNumber }
        assertEquals(setOf("1.1", "2.1"), queues.keys)
        assertTrue(queues.getValue("1.1").intervalsJson.contains("660"))
        assertEquals(first.sourceMessageKey, store.revisions.first().sourceMessageKey)
    }

    @Test
    fun repeatedMessageVersionIsIgnored() = runBlocking {
        val store = FakeScheduleHistoryStore()
        val service = ScheduleMessageIngestionService(historyStore = store, zoneId = zone)
        val incoming = message(
            id = 10L,
            sentAt = date.atTime(8, 0).atZone(zone).toInstant().toEpochMilli(),
            receivedAt = date.atTime(8, 1).atZone(zone).toInstant().toEpochMilli(),
            text = """
                Оновлено графік погодинних відключень на 23 жовтня
                Черга 1.1
                10:00–12:00
            """.trimIndent(),
        )

        assertEquals(IngestionStatus.APPLIED, service.ingest(incoming).status)
        assertEquals(IngestionStatus.DUPLICATE, service.ingest(incoming).status)
        assertEquals(1, store.revisions.size)
    }

    private fun message(
        id: Long,
        sentAt: Long,
        receivedAt: Long,
        text: String,
    ) = IncomingScheduleMessage(
        chatId = -100123L,
        telegramMessageId = id,
        text = text,
        sentAtEpochMillis = sentAt,
        receivedAtEpochMillis = receivedAt,
    )

    private class FakeScheduleHistoryStore : ScheduleHistoryStore {
        private val sources = linkedMapOf<String, SourceMessageEntity>()
        val revisions = mutableListOf<ScheduleRevisionEntity>()
        private val queues = linkedMapOf<Pair<String, String>, EffectiveQueueEntity>()

        override suspend fun hasSourceMessage(sourceMessageKey: String): Boolean =
            sourceMessageKey in sources

        override suspend fun recordSourceMessage(message: SourceMessageEntity) {
            sources[message.sourceMessageKey] = message
        }

        override suspend fun recordRevision(
            message: SourceMessageEntity,
            revision: ScheduleRevisionEntity,
        ) {
            sources[message.sourceMessageKey] = message
            revisions += revision
        }

        override suspend fun recordAcceptedRevision(
            message: SourceMessageEntity,
            revision: ScheduleRevisionEntity,
            effectiveQueues: List<EffectiveQueueEntity>,
        ) {
            sources[message.sourceMessageKey] = message
            revisions += revision
            effectiveQueues.forEach { queues[it.scheduleDate to it.queueNumber] = it }
        }

        override suspend fun saveEffectiveQueues(queues: List<EffectiveQueueEntity>) {
            queues.forEach { this.queues[it.scheduleDate to it.queueNumber] = it }
        }

        override fun observeSourceMessages(): Flow<List<SourceMessageEntity>> =
            flowOf(sources.values.toList())

        override fun observeRevisions(date: String): Flow<List<ScheduleRevisionEntity>> =
            flowOf(revisions.filter { it.scheduleDate == date })

        override fun observeEffectiveQueues(date: String): Flow<List<EffectiveQueueEntity>> =
            flowOf(getEffectiveQueues(date))

        override suspend fun getEffectiveQueues(date: String): List<EffectiveQueueEntity> =
            queues.filterKeys { it.first == date }.values.toList()
    }
}
