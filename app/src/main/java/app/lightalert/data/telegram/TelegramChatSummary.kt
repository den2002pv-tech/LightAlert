package app.lightalert.data.telegram

/**
 * Safe, UI-facing subset of a Telegram dialog.
 * Message preview is optional and must not be treated as a schedule.
 */
data class TelegramChatSummary(
    val id: Long,
    val title: String,
    val kind: String,
    val username: String?,
    val isArchived: Boolean,
    val isChannel: Boolean,
    val isSupergroup: Boolean,
    val isBasicGroup: Boolean,
    val unreadCount: Int,
    val lastMessageDateEpochSeconds: Long?,
    val lastMessagePreview: String?,
    val avatarPath: String?,
) {
    val displayName: String
        get() = if (title.isBlank()) username?.let { "@$it" } ?: "Без названия" else title

    val category: ChatCategory
        get() = when {
            isArchived -> ChatCategory.ARCHIVE
            isChannel -> ChatCategory.CHANNEL
            isSupergroup -> ChatCategory.SUPERGROUP
            isBasicGroup -> ChatCategory.GROUP
            else -> ChatCategory.OTHER
        }
}

enum class ChatCategory {
    GROUP,
    SUPERGROUP,
    CHANNEL,
    ARCHIVE,
    OTHER,
}
