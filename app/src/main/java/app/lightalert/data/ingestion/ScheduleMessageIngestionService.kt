package app.lightalert.data.ingestion

import app.lightalert.data.local.db.EffectiveQueueEntity
import app.lightalert.data.local.db.ScheduleHistoryStore
import app.lightalert.data.local.db.ScheduleRevisionEntity
import app.lightalert.data.local.db.SourceMessageEntity
import app.lightalert.domain.merger.ScheduleRevisionMerger
import app.lightalert.domain.model.EffectiveDailySchedule
import app.lightalert.domain.model.ParsedOutageInterval
import app.lightalert.domain.model.RevisionMergeStatus
import app.lightalert.domain.model.ScheduleRevision
import app.lightalert.domain.parser.ScheduleMessageParser
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Instant
import java.time.ZoneId

/**
 * Converts an incoming Telegram message into an auditable schedule revision.
 * TDLib is intentionally kept behind an adapter: this service can be tested
 * without a Telegram session or network connection.
 */
class ScheduleMessageIngestionService(
    private val historyStore: ScheduleHistoryStore,
    private val parser: ScheduleMessageParser = ScheduleMessageParser(),
    private val merger: ScheduleRevisionMerger = ScheduleRevisionMerger(),
    private val gson: Gson = Gson(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val zoneId: ZoneId = ZoneId.of("Europe/Kyiv"),
) {
    private val intervalListType = object : TypeToken<List<ParsedOutageInterval>>() {}.type

    suspend fun ingest(message: IncomingScheduleMessage): IngestionResult {
        val receivedAt = message.receivedAtEpochMillis ?: clock()
        val sourceKey = SourceMessageEntity.createVersionKey(
            chatId = message.chatId,
            telegramMessageId = message.telegramMessageId,
            sentAtEpochMillis = message.sentAtEpochMillis,
            editedAtEpochMillis = message.editedAtEpochMillis,
            receivedAtEpochMillis = receivedAt,
        )

        if (historyStore.hasSourceMessage(sourceKey)) {
            return IngestionResult(sourceKey, IngestionStatus.DUPLICATE)
        }

        val source = SourceMessageEntity(
            sourceMessageKey = sourceKey,
            chatId = message.chatId,
            telegramMessageId = message.telegramMessageId,
            messageText = message.text,
            sentAtEpochMillis = message.sentAtEpochMillis,
            receivedAtEpochMillis = receivedAt,
            editedAtEpochMillis = message.editedAtEpochMillis,
        )

        val yearTimestamp = message.sentAtEpochMillis ?: receivedAt
        val year = Instant.ofEpochMilli(yearTimestamp).atZone(zoneId).year
        val parsed = parser.parse(message.text, year)
        val schedule = parsed.schedule

        if (schedule == null) {
            historyStore.recordSourceMessage(source.copy(processingState = "UNPARSED"))
            return IngestionResult(sourceKey, IngestionStatus.UNPARSED, parsed.warnings)
        }

        val dateKey = schedule.date.toString()
        val storedQueues = historyStore.getEffectiveQueues(dateKey)
        val currentQueues = storedQueues.mapNotNull { row ->
            val intervals = runCatching {
                gson.fromJson<List<ParsedOutageInterval>>(row.intervalsJson, intervalListType)
            }.getOrNull().orEmpty()
            if (intervals.isEmpty()) null else row.queueNumber to intervals
        }.toMap()

        val lastUpdated = storedQueues.maxOfOrNull { it.updatedAtEpochMillis }
            ?.let(Instant::ofEpochMilli)
        val current = if (currentQueues.isEmpty() && lastUpdated == null) {
            null
        } else {
            EffectiveDailySchedule(
                date = schedule.date,
                queues = currentQueues,
                lastUpdatedAt = lastUpdated,
            )
        }

        val revision = ScheduleRevision(
            sourceMessageId = sourceKey,
            receivedAt = Instant.ofEpochMilli(receivedAt),
            schedule = schedule,
        )
        val merged = merger.merge(current, revision)
        val accepted = merged.status == RevisionMergeStatus.APPLIED

        val revisionEntity = ScheduleRevisionEntity(
            revisionKey = sourceKey,
            sourceMessageKey = sourceKey,
            scheduleDate = dateKey,
            queuesJson = gson.toJson(schedule.queues),
            warningsJson = gson.toJson(parsed.warnings),
            parsedAtEpochMillis = receivedAt,
            accepted = accepted,
        )

        if (!accepted) {
            historyStore.recordRevision(
                source.copy(processingState = merged.status.name),
                revisionEntity,
            )
            return IngestionResult(
                sourceMessageKey = sourceKey,
                status = IngestionStatus.valueOf(merged.status.name),
                warnings = parsed.warnings,
            )
        }

        // Only queues explicitly present with valid intervals are written.
        // Missing queues remain intact, preserving partial Telegram updates.
        val changedQueues = schedule.queues
            .filter { it.intervals.isNotEmpty() }
            .map { queue ->
                EffectiveQueueEntity(
                    scheduleDate = dateKey,
                    queueNumber = queue.number,
                    intervalsJson = gson.toJson(queue.intervals),
                    sourceMessageKey = sourceKey,
                    updatedAtEpochMillis = receivedAt,
                )
            }

        historyStore.recordAcceptedRevision(
            message = source.copy(processingState = "APPLIED"),
            revision = revisionEntity,
            effectiveQueues = changedQueues,
        )

        return IngestionResult(sourceKey, IngestionStatus.APPLIED, parsed.warnings)
    }
}

data class IncomingScheduleMessage(
    val chatId: Long,
    val telegramMessageId: Long,
    val text: String,
    val sentAtEpochMillis: Long?,
    val editedAtEpochMillis: Long? = null,
    val receivedAtEpochMillis: Long? = null,
)

data class IngestionResult(
    val sourceMessageKey: String,
    val status: IngestionStatus,
    val warnings: List<String> = emptyList(),
)

enum class IngestionStatus {
    APPLIED,
    DUPLICATE,
    STALE,
    DATE_MISMATCH,
    NO_VALID_QUEUE_DATA,
    UNPARSED,
}
