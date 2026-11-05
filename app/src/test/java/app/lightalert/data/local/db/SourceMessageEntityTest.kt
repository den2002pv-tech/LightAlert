package app.lightalert.data.local.db

import org.junit.Assert.assertEquals
import org.junit.Test

class SourceMessageEntityTest {
    @Test
    fun editedMessageGetsDifferentVersionKey() {
        val original = SourceMessageEntity.createVersionKey(
            chatId = -100123L,
            telegramMessageId = 456L,
            sentAtEpochMillis = 1_000L,
            editedAtEpochMillis = null,
            receivedAtEpochMillis = 1_100L,
        )
        val edited = SourceMessageEntity.createVersionKey(
            chatId = -100123L,
            telegramMessageId = 456L,
            sentAtEpochMillis = 1_000L,
            editedAtEpochMillis = 2_000L,
            receivedAtEpochMillis = 2_100L,
        )

        assertEquals("-100123:456:1000", original)
        assertEquals("-100123:456:2000", edited)
    }

    @Test
    fun duplicateDeliveryOfSameVersionGetsSameKey() {
        val first = SourceMessageEntity.createVersionKey(
            chatId = -100123L,
            telegramMessageId = 456L,
            sentAtEpochMillis = 1_000L,
            editedAtEpochMillis = 2_000L,
            receivedAtEpochMillis = 2_100L,
        )
        val redelivered = SourceMessageEntity.createVersionKey(
            chatId = -100123L,
            telegramMessageId = 456L,
            sentAtEpochMillis = 1_000L,
            editedAtEpochMillis = 2_000L,
            receivedAtEpochMillis = 9_999L,
        )

        assertEquals(first, redelivered)
    }
}
