package app.lightalert.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * Current effective schedule for one date.
 *
 * Queue data is keyed by queue number. A queue absent from a source message
 * remains untouched. Source IDs and timestamps make merging idempotent and
 * protect the effective state from older revisions arriving late.
 */
data class EffectiveDailySchedule(
    val date: LocalDate,
    val queues: Map<String, List<ParsedOutageInterval>> = emptyMap(),
    val lastUpdatedAt: Instant? = null,
    val processedSourceMessageIds: Set<String> = emptySet(),
)

data class ScheduleRevision(
    val sourceMessageId: String,
    val receivedAt: Instant,
    val schedule: ParsedScheduleMessage,
)

enum class RevisionMergeStatus {
    APPLIED,
    DUPLICATE,
    STALE,
    DATE_MISMATCH,
    NO_VALID_QUEUE_DATA,
}

data class RevisionMergeResult(
    val schedule: EffectiveDailySchedule,
    val status: RevisionMergeStatus,
)
