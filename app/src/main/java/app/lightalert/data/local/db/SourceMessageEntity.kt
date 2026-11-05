package app.lightalert.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Immutable version of a source payload captured before parsing.
 *
 * sourceMessageKey identifies a message version, not merely a Telegram message.
 * Use createVersionKey so edits to the same Telegram message retain their own
 * source record and can be audited independently.
 */
@Entity(
    tableName = "source_messages",
    indices = [
        Index(value = ["chatId", "telegramMessageId"]),
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
) {
    companion object {
        /**
         * Stable key for a particular Telegram message version.
         * Telegram's edit timestamp distinguishes edited versions; sent time
         * identifies the original message. Receive time is the final fallback.
         */
        fun createVersionKey(
            chatId: Long,
            telegramMessageId: Long,
            sentAtEpochMillis: Long?,
            editedAtEpochMillis: Long?,
            receivedAtEpochMillis: Long,
        ): String {
            val versionTimestamp = editedAtEpochMillis
                ?: sentAtEpochMillis
                ?: receivedAtEpochMillis
            return "$chatId:$telegramMessageId:$versionTimestamp"
        }
    }
}
