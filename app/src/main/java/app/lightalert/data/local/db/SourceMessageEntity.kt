package app.lightalert.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Raw source payload captured before parsing.
 * sourceMessageKey is normally the stable composite key "<chatId>:<messageId>".
 */
@Entity(
    tableName = "source_messages",
    indices = [
        Index(value = ["chatId", "telegramMessageId"], unique = true),
        Index(value = ["receivedAtEpochMillis"]),
    ],
)
data class SourceMessageEntity(
    @PrimaryKey
    val sourceMessageKey: String,
    val chatId: Long?,
    val telegramMessageId: Long?,
    val messageText: String,
    val sentAtEpochMillis: Long?,
    val receivedAtEpochMillis: Long,
    val editedAtEpochMillis: Long?,
    val processingState: String = "RECEIVED",
)
