package app.lightalert.data.telegram

/**
 * Safe, UI-facing subset of a Telegram dialog.
 * Message preview is optional and must not be treated as a schedule.
 */
data class TelegramChatSummary(
    val id: Long,
    val title: String,
    val unreadCount: Int,
    val lastMessageDateEpochSeconds: Long?,
    val lastMessagePreview: String?,
)
