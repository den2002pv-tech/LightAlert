package app.lightalert.data.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "schedule_revisions",
    foreignKeys = [
        ForeignKey(
            entity = SourceMessageEntity::class,
            parentColumns = ["sourceMessageKey"],
            childColumns = ["sourceMessageKey"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["scheduleDate"]),
        Index(value = ["parsedAtEpochMillis"]),
    ],
)
data class ScheduleRevisionEntity(
    @PrimaryKey
    val sourceMessageKey: String,
    val scheduleDate: String,
    /** JSON array of parsed queues and their minute-based intervals. */
    val queuesJson: String,
    val warningsJson: String,
    val parsedAtEpochMillis: Long,
    val accepted: Boolean,
)
