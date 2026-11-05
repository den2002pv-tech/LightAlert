package app.lightalert.data.local.db

import kotlinx.coroutines.flow.Flow

/**
 * Persistence boundary for original messages, parsed revisions and effective queues.
 * Implementations must not delete effective queues omitted from a partial update.
 */
interface ScheduleHistoryStore {
    suspend fun recordSourceMessage(message: SourceMessageEntity)

    suspend fun recordRevision(
        message: SourceMessageEntity,
        revision: ScheduleRevisionEntity,
    )

    /**
     * Atomically stores the source message, accepted parsed revision and the
     * effective queue rows produced by the merger. All queue rows must belong
     * to this revision's date and source message.
     */
    suspend fun recordAcceptedRevision(
        message: SourceMessageEntity,
        revision: ScheduleRevisionEntity,
        effectiveQueues: List<EffectiveQueueEntity>,
    )

    suspend fun saveEffectiveQueues(queues: List<EffectiveQueueEntity>)

    fun observeSourceMessages(): Flow<List<SourceMessageEntity>>

    fun observeRevisions(date: String): Flow<List<ScheduleRevisionEntity>>

    fun observeEffectiveQueues(date: String): Flow<List<EffectiveQueueEntity>>

    suspend fun getEffectiveQueues(date: String): List<EffectiveQueueEntity>
}
