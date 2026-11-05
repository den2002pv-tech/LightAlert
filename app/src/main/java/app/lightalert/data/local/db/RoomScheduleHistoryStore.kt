package app.lightalert.data.local.db

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class RoomScheduleHistoryStore(
    private val database: LightAlertDatabase,
) : ScheduleHistoryStore {
    private val dao = database.lightAlertDao()

    override suspend fun hasSourceMessage(sourceMessageKey: String): Boolean =
        dao.getSourceMessage(sourceMessageKey) != null

    override suspend fun recordSourceMessage(message: SourceMessageEntity) {
        dao.upsertSourceMessage(message)
    }

    override suspend fun recordRevision(
        message: SourceMessageEntity,
        revision: ScheduleRevisionEntity,
    ) {
        require(message.sourceMessageKey == revision.sourceMessageKey) {
            "Revision must reference the source message version being saved"
        }
        database.withTransaction {
            dao.upsertSourceMessage(message)
            dao.upsertScheduleRevision(revision)
        }
    }

    override suspend fun recordAcceptedRevision(
        message: SourceMessageEntity,
        revision: ScheduleRevisionEntity,
        effectiveQueues: List<EffectiveQueueEntity>,
    ) {
        require(message.sourceMessageKey == revision.sourceMessageKey) {
            "Revision must reference the source message version being saved"
        }
        require(revision.accepted) {
            "Only accepted revisions may update the effective schedule"
        }
        require(effectiveQueues.all { queue ->
            queue.scheduleDate == revision.scheduleDate &&
                queue.sourceMessageKey == message.sourceMessageKey
        }) {
            "Effective queues must match the revision date and source message version"
        }
        database.withTransaction {
            dao.upsertSourceMessage(message)
            dao.upsertScheduleRevision(revision)
            if (effectiveQueues.isNotEmpty()) dao.upsertEffectiveQueues(effectiveQueues)
        }
    }

    override suspend fun saveEffectiveQueues(queues: List<EffectiveQueueEntity>) {
        if (queues.isEmpty()) return
        require(queues.map { it.scheduleDate }.distinct().size == 1) {
            "A single write must contain queues for exactly one schedule date"
        }
        database.withTransaction { dao.upsertEffectiveQueues(queues) }
    }

    override fun observeSourceMessages(): Flow<List<SourceMessageEntity>> =
        dao.observeSourceMessages()

    override fun observeRevisions(date: String): Flow<List<ScheduleRevisionEntity>> =
        dao.observeRevisions(date)

    override fun observeEffectiveQueues(date: String): Flow<List<EffectiveQueueEntity>> =
        dao.observeEffectiveQueues(date)

    override suspend fun getEffectiveQueues(date: String): List<EffectiveQueueEntity> =
        dao.getEffectiveQueues(date)
}
