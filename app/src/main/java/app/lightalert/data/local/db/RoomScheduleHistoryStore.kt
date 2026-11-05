package app.lightalert.data.local.db

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class RoomScheduleHistoryStore(
    private val database: LightAlertDatabase,
) : ScheduleHistoryStore {
    private val dao = database.lightAlertDao()

    override suspend fun recordSourceMessage(message: SourceMessageEntity) {
        dao.upsertSourceMessage(message)
    }

    override suspend fun recordRevision(
        message: SourceMessageEntity,
        revision: ScheduleRevisionEntity,
    ) {
        require(message.sourceMessageKey == revision.sourceMessageKey) {
            "Revision must reference the source message being saved"
        }

        database.withTransaction {
            dao.upsertSourceMessage(message)
            dao.upsertScheduleRevision(revision)
        }
    }

    override suspend fun saveEffectiveQueues(queues: List<EffectiveQueueEntity>) {
        if (queues.isEmpty()) return
        database.withTransaction {
            dao.upsertEffectiveQueues(queues)
        }
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
