package app.lightalert.data.local.db

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "effective_queues",
    primaryKeys = ["scheduleDate", "queueNumber"],
    indices = [Index(value = ["sourceMessageKey"])],
)
data class EffectiveQueueEntity(
    val scheduleDate: String,
    val queueNumber: String,
    /** JSON array of intervals; minute 1440 represents 24:00. */
    val intervalsJson: String,
    val sourceMessageKey: String?,
    val updatedAtEpochMillis: Long,
)
