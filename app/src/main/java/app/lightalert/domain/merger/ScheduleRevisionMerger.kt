package app.lightalert.domain.merger

import app.lightalert.domain.model.EffectiveDailySchedule
import app.lightalert.domain.model.RevisionMergeResult
import app.lightalert.domain.model.RevisionMergeStatus
import app.lightalert.domain.model.ScheduleRevision

/**
 * Applies a parsed source revision to an effective daily schedule.
 *
 * A revision replaces only queues containing at least one valid interval.
 * Missing queues and queues with no valid intervals are deliberately ignored.
 */
class ScheduleRevisionMerger {

    fun merge(
        current: EffectiveDailySchedule?,
        revision: ScheduleRevision,
    ): RevisionMergeResult {
        val existing = current ?: EffectiveDailySchedule(date = revision.schedule.date)

        if (existing.date != revision.schedule.date) {
            return RevisionMergeResult(existing, RevisionMergeStatus.DATE_MISMATCH)
        }

        if (revision.sourceMessageId in existing.processedSourceMessageIds) {
            return RevisionMergeResult(existing, RevisionMergeStatus.DUPLICATE)
        }

        val lastUpdatedAt = existing.lastUpdatedAt
        if (lastUpdatedAt != null && revision.receivedAt.isBefore(lastUpdatedAt)) {
            return RevisionMergeResult(existing, RevisionMergeStatus.STALE)
        }

        val validQueues = revision.schedule.queues
            .filter { it.intervals.isNotEmpty() }
            .associate { queue -> queue.number to queue.intervals }

        if (validQueues.isEmpty()) {
            return RevisionMergeResult(existing, RevisionMergeStatus.NO_VALID_QUEUE_DATA)
        }

        val updated = existing.copy(
            queues = existing.queues + validQueues,
            lastUpdatedAt = revision.receivedAt,
            processedSourceMessageIds = existing.processedSourceMessageIds + revision.sourceMessageId,
        )

        return RevisionMergeResult(updated, RevisionMergeStatus.APPLIED)
    }
}
