package app.lightalert.data.telegram

import android.content.Context
import android.os.Build
import app.lightalert.BuildConfig
import app.lightalert.data.ingestion.IncomingScheduleMessage
import app.lightalert.data.ingestion.IngestionResult
import app.lightalert.data.ingestion.ScheduleMessageIngestionService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Owns the on-device TDLib session and forwards selected text-channel messages
 * into the schedule ingestion pipeline.
 *
 * TDLib is initialized through its low-level Java client so the database
 * encryption key is supplied by [TdlibDatabaseKeyStore], not by a wrapper
 * that hardcodes an empty key.
 */
class TdlibTelegramClient(
    context: Context,
    private val ingestionService: ScheduleMessageIngestionService,
    private val apiConfigStore: TelegramApiConfigStore,
    private val chatSelectionStore: TelegramChatSelectionStore,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val databaseKeyStore = TdlibDatabaseKeyStore(appContext)
    private val updates = Channel<TdApi.Update>(Channel.UNLIMITED)

    private val _authState = MutableStateFlow(TelegramAuthState.NOT_CONFIGURED)
    val authState: StateFlow<TelegramAuthState> = _authState.asStateFlow()

    private val _ingestionResults = MutableSharedFlow<IngestionResult>(extraBufferCapacity = 32)
    val ingestionResults: SharedFlow<IngestionResult> = _ingestionResults.asSharedFlow()

    @Volatile
    private var selectedChatIds: Set<Long> = emptySet()

    @Volatile
    private var client: Client? = null

    init {
        scope.launch {
            for (update in updates) {
                runCatching { handleUpdate(update) }
                    .onFailure { _authState.value = TelegramAuthState.ERROR }
            }
        }
        scope.launch {
            chatSelectionStore.selectedChatIds.collect { ids ->
                setSelectedChatIds(ids)
            }
        }
    }

    @Synchronized
    fun start(): Boolean {
        if (client != null) return true
        val apiConfig = runCatching { apiConfigStore.get() }.getOrNull()
        if (apiConfig == null || apiConfig.apiId <= 0 || apiConfig.apiHash.isBlank()) {
            _authState.value = TelegramAuthState.NOT_CONFIGURED
            return false
        }

        return runCatching {
            System.loadLibrary("tdjni")
            Client.execute(TdApi.SetLogVerbosityLevel(0))
            client = Client.create(
                { update ->
                    if (update is TdApi.Update) updates.trySend(update)
                },
                { _authState.value = TelegramAuthState.ERROR },
                { _authState.value = TelegramAuthState.ERROR },
            )
            _authState.value = TelegramAuthState.INITIALIZING
            client?.send(TdApi.GetAuthorizationState()) { result ->
                when (result) {
                    is TdApi.AuthorizationState ->
                        updates.trySend(TdApi.UpdateAuthorizationState(result))
                    is TdApi.Error -> _authState.value = TelegramAuthState.ERROR
                }
            }
            true
        }.getOrElse {
            client = null
            _authState.value = TelegramAuthState.ERROR
            false
        }
    }

    /**
     * Loads the most recent dialogs from Telegram for the channel-selection UI.
     * This method is deliberately read-only: it does not subscribe to any chat.
     */
    suspend fun loadChats(limit: Int = 100): List<TelegramChatSummary> {
        require(limit in 1..500) { "Chat limit must be between 1 and 500" }
        check(authState.value == TelegramAuthState.READY) {
            "Telegram authorization is required before loading chats"
        }

        // GetChats only returns dialogs that TDLib has already loaded into its
        // local cache. Explicitly load both active and archived dialog lists first.
        // A 404 from LoadChats means there are no more dialogs in that list.
        val chatLists = listOf(TdApi.ChatListMain(), TdApi.ChatListArchive())
        chatLists.forEach { chatList ->
            runCatching { send(TdApi.LoadChats(chatList, limit)) }
        }

        val chatIds = chatLists.flatMap { chatList ->
            runCatching { send(TdApi.GetChats(chatList, limit)).chatIds.toList() }
                .getOrDefault(emptyList())
        }.distinct()

        return chatIds.mapNotNull { chatId: Long ->
            runCatching { send(TdApi.GetChat(chatId)) as TdApi.Chat }.getOrNull()
        }.filter { chat ->
            chat.type is TdApi.ChatTypeBasicGroup || chat.type is TdApi.ChatTypeSupergroup
        }.map { chat ->
            val chatType = chat.type
            val isChannel = (chatType as? TdApi.ChatTypeSupergroup)?.isChannel == true
            val isSupergroup = chatType is TdApi.ChatTypeSupergroup && !isChannel
            val isBasicGroup = chatType is TdApi.ChatTypeBasicGroup
            val isArchived = chat.isArchived == true

            // Try to get profile photo path
            val avatarPath = if (chat.photo != null && chat.photo.small != null) {
                chat.photo.small.local.path.takeIf { it.isNotBlank() }
            } else null

            TelegramChatSummary(
                id = chat.id,
                title = chat.title,
                kind = when {
                    isChannel -> "Канал"
                    isSupergroup -> "Супергруппа"
                    isBasicGroup -> "Группа"
                    else -> "Чат"
                },
                username = chat.usernames?.firstOrNull()?.username,
                isArchived = isArchived,
                isChannel = isChannel,
                isSupergroup = isSupergroup,
                isBasicGroup = isBasicGroup,
                unreadCount = chat.unreadCount,
                lastMessageDateEpochSeconds = chat.lastMessage?.date?.toLong(),
                lastMessagePreview = when (val content = chat.lastMessage?.content) {
                    is TdApi.MessageText -> content.text.text
                    else -> null
                },
                avatarPath = avatarPath,
            )
        }
    }

    fun setSelectedChatIds(chatIds: Set<Long>) {
        val previous = selectedChatIds
        selectedChatIds = chatIds.toSet()
        applyChatSelection(previous, selectedChatIds)
    }

    private fun applyChatSelection(previous: Set<Long>, current: Set<Long>) {
        val activeClient = client ?: return
        (current - previous).forEach { chatId ->
            activeClient.send(TdApi.OpenChat(chatId)) { result ->
                if (result is TdApi.Error) _authState.value = TelegramAuthState.ERROR
            }
        }
        (previous - current).forEach { chatId ->
            activeClient.send(TdApi.CloseChat(chatId), null)
        }
    }

    private fun openAllSelectedChats() {
        applyChatSelection(emptySet(), selectedChatIds)
    }

    fun refreshAuthorizationState() {
        val activeClient = client
        if (activeClient == null) {
            start()
            return
        }
        activeClient.send(TdApi.GetAuthorizationState()) { result ->
            when (result) {
                is TdApi.AuthorizationState ->
                    updates.trySend(TdApi.UpdateAuthorizationState(result))
                is TdApi.Error -> _authState.value = TelegramAuthState.ERROR
            }
        }
    }

    suspend fun submitPhoneNumber(phoneNumber: String) {
        require(phoneNumber.isNotBlank()) { "Phone number must not be blank" }
        require(phoneNumber.length <= 32) { "Phone number is too long" }
        require(phoneNumber.all {
            it.isDigit() || it == '+' || it == ' ' || it == '-' || it == '(' || it == ')'
        }) { "Phone number contains unsupported characters" }
        requireAuthState(TelegramAuthState.WAITING_FOR_PHONE)
        send(TdApi.SetAuthenticationPhoneNumber(phoneNumber.trim(), null))
    }

    suspend fun submitCode(code: String) {
        require(code.isNotBlank()) { "Authentication code must not be blank" }
        require(code.length <= 16) { "Authentication code is too long" }
        requireAuthState(TelegramAuthState.WAITING_FOR_CODE)
        send(TdApi.CheckAuthenticationCode(code.trim()))
    }

    suspend fun submitPassword(password: String) {
        require(password.isNotBlank()) { "Password must not be blank" }
        require(password.length <= 256) { "Password is too long" }
        requireAuthState(TelegramAuthState.WAITING_FOR_PASSWORD)
        send(TdApi.CheckAuthenticationPassword(password))
    }

    suspend fun logOut() {
        send(TdApi.LogOut())
    }

    @Synchronized
    fun stop() {
        val activeClient = client
        client = null
        runCatching { activeClient?.send(TdApi.Close(), null) }
        _authState.value = TelegramAuthState.CLOSED
    }

    fun release() {
        stop()
        updates.close()
        scope.cancel()
    }

    private suspend fun handleUpdate(update: TdApi.Update) {
        when (update) {
            is TdApi.UpdateAuthorizationState -> {
                val state = update.authorizationState
                _authState.value = state.toLightAlertState()
                if (state is TdApi.AuthorizationStateReady) openAllSelectedChats()
                if (state is TdApi.AuthorizationStateWaitTdlibParameters) {
                    initializeTdlibParameters()
                }
            }

            is TdApi.UpdateNewMessage -> ingestIfSelected(update.message)

            is TdApi.UpdateMessageContent -> {
                val refreshed = runCatching {
                    send(TdApi.GetMessage(update.chatId, update.messageId))
                }.getOrNull()
                if (refreshed is TdApi.Message) ingestIfSelected(refreshed)
            }
        }
    }

    private suspend fun initializeTdlibParameters() {
        val tdlibDirectory = File(appContext.filesDir, "tdlib-secure").apply { mkdirs() }
        val filesDirectory = File(tdlibDirectory, "files").apply { mkdirs() }
        val databaseKey = databaseKeyStore.getOrCreateDatabaseKey()
        val apiConfig = checkNotNull(apiConfigStore.get()) {
            "Telegram API configuration is missing"
        }

        send(
            TdApi.SetTdlibParameters(
                false,
                tdlibDirectory.absolutePath,
                filesDirectory.absolutePath,
                databaseKey,
                true,
                true,
                true,
                false,
                apiConfig.apiId,
                apiConfig.apiHash,
                Locale.getDefault().toLanguageTag().ifBlank { "en" },
                Build.MODEL?.takeIf(String::isNotBlank) ?: "Android",
                Build.VERSION.RELEASE.orEmpty(),
                BuildConfig.VERSION_NAME,
            ),
        )
    }

    private suspend fun ingestIfSelected(message: TdApi.Message) {
        if (message.chatId !in selectedChatIds) return
        val textContent = message.content as? TdApi.MessageText ?: return
        val text = textContent.text.text
        if (text.isBlank()) return

        val sentAt = message.date.toLong() * 1_000L
        val editedAt = message.editDate
            .takeIf { it > 0 }
            ?.toLong()
            ?.times(1_000L)

        val result = runCatching {
            ingestionService.ingest(
                IncomingScheduleMessage(
                    chatId = message.chatId,
                    telegramMessageId = message.id,
                    text = text,
                    sentAtEpochMillis = sentAt,
                    editedAtEpochMillis = editedAt,
                ),
            )
        }.getOrNull() ?: return

        _ingestionResults.emit(result)
    }

    private suspend fun <T : TdApi.Object> send(function: TdApi.Function<T>): T =
        suspendCancellableCoroutine { continuation ->
            val activeClient = client
            if (activeClient == null) {
                continuation.resumeWithException(
                    IllegalStateException("Telegram client is not started"),
                )
                return@suspendCancellableCoroutine
            }

            activeClient.send(function) { result ->
                if (continuation.isActive) {
                    if (result is TdApi.Error) {
                        continuation.resumeWithException(
                            TdlibRequestException(result.code, result.message),
                        )
                    } else {
                        @Suppress("UNCHECKED_CAST")
                        continuation.resume(result as T)
                    }
                }
            }
        }

    private fun requireAuthState(expected: TelegramAuthState) {
        check(authState.value == expected) {
            "Authentication action is not valid in state ${authState.value}"
        }
    }

    /**
     * Search for public Telegram chats and channels by title or username.
     * Returns a list of public chats sorted by relevance.
     */
    suspend fun searchPublicChats(query: String): List<TelegramChatSummary> {
        require(query.isNotBlank()) { "Search query must not be blank" }
        check(authState.value == TelegramAuthState.READY) {
            "Telegram authorization is required before searching"
        }

        val result = runCatching { send(TdApi.SearchChats(query)) }.getOrNull()
        return when (result) {
            is TdApi.Chats -> result.chats
                .filter { chat ->
                    (chat.type is TdApi.ChatTypeBasicGroup || chat.type is TdApi.ChatTypeSupergroup) &&
                        (chat.username != null || chat.title != null)
                }
                .map { chat ->
                    val isChannel = (chat.type as? TdApi.ChatTypeSupergroup)?.isChannel == true
                    TelegramChatSummary(
                        id = chat.id,
                        title = chat.title,
                        kind = if (isChannel) "Канал" else "Группа",
                        unreadCount = chat.unreadCount,
                        lastMessageDateEpochSeconds = chat.lastMessage?.date?.toLong(),
                        lastMessagePreview = when (val content = chat.lastMessage?.content) {
                            is TdApi.MessageText -> content.text.text
                            else -> null
                        },
                        avatarPath = null,
                    )
                }
            else -> emptyList()
        }
    }

    /**
     * Search for a specific public Telegram chat or channel by username.
     * Returns the first matching public chat.
     */
    suspend fun searchPublicChat(query: String): TelegramChatSummary? {
        require(query.isNotBlank()) { "Search query must not be blank" }
        check(authState.value == TelegramAuthState.READY) {
            "Telegram authorization is required before searching"
        }

        val result = runCatching { send(TdApi.SearchPublicChat(query)) }.getOrNull()
        return when (result) {
            is TdApi.Chat -> {
                val isChannel = result.type is TdApi.ChatTypeSupergroup && result.type.isChannel
                TelegramChatSummary(
                    id = result.id,
                    title = result.title,
                    kind = if (isChannel) "Канал" else "Группа",
                    unreadCount = result.unreadCount,
                    lastMessageDateEpochSeconds = result.lastMessage?.date?.toLong(),
                    lastMessagePreview = when (val content = result.lastMessage?.content) {
                        is TdApi.MessageText -> content.text.text
                        else -> null
                    },
                    avatarPath = null,
                )
            }
            else -> null
        }
    }

    /**
     * Join a Telegram chat by its public username (e.g., "t.me/username").
     * This works for public channels and groups that don't require an invite.
     */
    suspend fun joinChatByUsername(username: String): TelegramChatSummary? {
        require(username.isNotBlank()) { "Username must not be blank" }
        check(authState.value == TelegramAuthState.READY) {
            "Telegram authorization is required before joining a chat"
        }

        val result = runCatching { send(TdApi.JoinChat(username)) }.getOrNull()
        return when (result) {
            is TdApi.Chat -> {
                val isChannel = result.type is TdApi.ChatTypeSupergroup && result.type.isChannel
                TelegramChatSummary(
                    id = result.id,
                    title = result.title,
                    kind = if (isChannel) "Канал" else "Группа",
                    unreadCount = result.unreadCount,
                    lastMessageDateEpochSeconds = result.lastMessage?.date?.toLong(),
                    lastMessagePreview = when (val content = result.lastMessage?.content) {
                        is TdApi.MessageText -> content.text.text
                        else -> null
                    },
                    avatarPath = null,
                )
            }
            is TdApi.Error -> {
                throw TdlibRequestException(result.code, result.message)
            }
            else -> null
        }
    }

    /**
     * Join a Telegram chat by its invite link (e.g., "t.me/+invite_hash").
     * This works for private groups that require an invite link.
     */
    suspend fun joinChatByInviteLink(inviteLink: String): TelegramChatSummary? {
        require(inviteLink.isNotBlank()) { "Invite link must not be blank" }
        check(authState.value == TelegramAuthState.READY) {
            "Telegram authorization is required before joining a chat by invite link"
        }

        val result = runCatching { send(TdApi.JoinChatByInviteLink(inviteLink)) }.getOrNull()
        return when (result) {
            is TdApi.Chat -> {
                val isChannel = result.type is TdApi.ChatTypeSupergroup && result.type.isChannel
                TelegramChatSummary(
                    id = result.id,
                    title = result.title,
                    kind = if (isChannel) "Канал" else "Группа",
                    unreadCount = result.unreadCount,
                    lastMessageDateEpochSeconds = result.lastMessage?.date?.toLong(),
                    lastMessagePreview = when (val content = result.lastMessage?.content) {
                        is TdApi.MessageText -> content.text.text
                        else -> null
                    },
                    avatarPath = null,
                )
            }
            is TdApi.Error -> {
                throw TdlibRequestException(result.code, result.message)
            }
            else -> null
        }
    }

    private fun TdApi.AuthorizationState.toLightAlertState(): TelegramAuthState = when (this) {
        is TdApi.AuthorizationStateWaitTdlibParameters -> TelegramAuthState.INITIALIZING
        is TdApi.AuthorizationStateWaitPhoneNumber -> TelegramAuthState.WAITING_FOR_PHONE
        is TdApi.AuthorizationStateWaitCode -> TelegramAuthState.WAITING_FOR_CODE
        is TdApi.AuthorizationStateWaitPassword -> TelegramAuthState.WAITING_FOR_PASSWORD
        is TdApi.AuthorizationStateWaitOtherDeviceConfirmation ->
            TelegramAuthState.WAITING_FOR_OTHER_DEVICE_CONFIRMATION
        is TdApi.AuthorizationStateReady -> TelegramAuthState.READY
        is TdApi.AuthorizationStateLoggingOut -> TelegramAuthState.CLOSING
        is TdApi.AuthorizationStateClosing -> TelegramAuthState.CLOSING
        is TdApi.AuthorizationStateClosed -> TelegramAuthState.CLOSED
        else -> TelegramAuthState.UNKNOWN
    }
}

class TdlibRequestException(
    val code: Int,
    override val message: String,
) : Exception(message)

enum class TelegramAuthState {
    NOT_CONFIGURED,
    INITIALIZING,
    WAITING_FOR_PHONE,
    WAITING_FOR_CODE,
    WAITING_FOR_PASSWORD,
    WAITING_FOR_OTHER_DEVICE_CONFIRMATION,
    READY,
    CLOSING,
    CLOSED,
    ERROR,
    UNKNOWN,
}


