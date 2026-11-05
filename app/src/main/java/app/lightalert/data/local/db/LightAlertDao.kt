package app.lightalert.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LightAlertDao {
    @Upsert
    suspend fun upsertSourceMessage(message: SourceMessageEntity)

    @Upsert
    suspend fun upsertScheduleRevision(revision: ScheduleRevisionEntity)

    /**
     * Upserts only supplied queues. Omitted queues are intentionally untouched,
     * which is essential for partial Telegram updates.
     */
    @Upsert
    suspend fun upsertEffectiveQueues(queues: List<EffectiveQueueEntity>)

    @Query("SELECT * FROM source_messages ORDER BY receivedAtEpochMillis DESC")
    fun observeSourceMessages(): Flow<List<SourceMessageEntity>>

    @Query("SELECT * FROM source_messages WHERE sourceMessageKey = :key LIMIT 1")
    suspend fun getSourceMessage(key: String): SourceMessageEntity?

    @Query(
        "SELECT * FROM schedule_revisions " +
            "WHERE scheduleDate = :date ORDER BY parsedAtEpochMillis DESC"
    )
    fun observeRevisions(date: String): Flow<List<ScheduleRevisionEntity>>

    @Query(
        "SELECT * FROM effective_queues " +
            "WHERE scheduleDate = :date ORDER BY queueNumber ASC"
    )
    fun observeEffectiveQueues(date: String): Flow<List<EffectiveQueueEntity>>

    @Query("SELECT * FROM effective_queues WHERE scheduleDate = :date")
    suspend fun getEffectiveQueues(date: String): List<EffectiveQueueEntity>
}
