package app.lightalert.data.telegram

import android.content.Context
import app.lightalert.BuildConfig
import app.lightalert.data.ingestion.IncomingScheduleMessage
import app.lightalert.data.ingestion.IngestionResult
import app.lightalert.data.ingestion.ScheduleMessageIngestionService
import io.github.tdlibandroid.ktx.TdClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import java.io.File

/**
 * Owns the on-device TDLib session and forwards selected text-channel messages
 * into the existing schedule ingestion pipeline.
 *
 * This class deliberately does not request phone numbers or authentication
 * codes itself. The UI must collect [authState] and submit those values through
 * the explicit methods below.
 */
class TdlibTelegramClient(
    context: Context,
    private val ingestionService: ScheduleMessageIngestionService,
    private val apiId: Int = BuildConfig.TELEGRAM_API_ID,
    private val apiHash: String = BuildConfig.TELEGRAM_API_HASH,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val _authState = MutableStateFlow(TelegramAuthState.NOT_CONFIGURED)
    val authState: StateFlow<TelegramAuthState> = _authState.asStateFlow()

    private val _ingestionResults = MutableSharedFlow<IngestionResult>(extraBufferCapacity = 32)
    val ingestionResults: SharedFlow<IngestionResult> = _ingestionResults.asSharedFlow()

    @Volatile
    private var selectedChatIds: Set<Long> = emptySet()

    @Volatile
    private var client: TdClient? = null

    fun start(): Boolean {
        if (client != null) return true
        if (apiId <= 0 || apiHash.isBlank()) {
            _authState.value = TelegramAuthState.NOT_CONFIGURED
            return false
        }

        return runCatching {
            System.loadLibrary("tdjni")
            val tdClient = TdClient(
                filesDir = File(appContext.filesDir, "tdlib").absolutePath,
                verbosityLevel = 1,
                apiId = apiId,
                apiHash = apiHash,
            )
            client = tdClient

            // Subscribe before init() so the first authorization update is not lost.
            scope.launch {
                tdClient.updates.collect { update -> handleUpdate(update) }
            }
            tdClient.init()
            _authState.value = TelegramAuthState.INITIALIZING
            true
        }.getOrElse { error ->
            client = null
            _authState.value = TelegramAuthState.ERROR
            false
        }
    }

    fun setSelectedChatIds(chatIds: Set<Long>) {
        selectedChatIds = chatIds.toSet()
    }

    suspend fun submitPhoneNumber(phoneNumber: String) {
        require(phoneNumber.isNotBlank()) { "Phone number must not be blank" }
        require(phoneNumber.length <= 32) { "Phone number is too long" }
        require(phoneNumber.all { it.isDigit() || it == '+' || it == ' ' || it == '-' || it == '(' || it == ')' }) {
            "Phone number contains unsupported characters"
        }
        requireAuthState(TelegramAuthState.WAITING_FOR_PHONE)
        requireClient().send(TdApi.SetAuthenticationPhoneNumber(phoneNumber.trim(), null))
    }

    suspend fun submitCode(code: String) {
        require(code.isNotBlank()) { "Authentication code must not be blank" }
        require(code.length <= 16) { "Authentication code is too long" }
        requireAuthState(TelegramAuthState.WAITING_FOR_CODE)
        requireClient().send(TdApi.CheckAuthenticationCode(code.trim()))
    }

    suspend fun submitPassword(password: String) {
        require(password.isNotBlank()) { "Password must not be blank" }
        require(password.length <= 256) { "Password is too long" }
        requireAuthState(TelegramAuthState.WAITING_FOR_PASSWORD)
        requireClient().send(TdApi.CheckAuthenticationPassword(password))
    }

    suspend fun logOut() {
        requireClient().send(TdApi.LogOut())
    }

    fun stop() {
        client?.close()
        client = null
        _authState.value = TelegramAuthState.CLOSED
    }

    fun release() {
        stop()
        scope.cancel()
    }

    private suspend fun handleUpdate(update: TdApi.Update) {
        when (update) {
            is TdApi.UpdateAuthorizationState -> {
                _authState.value = update.authorizationState.toLightAlertState()
            }

            is TdApi.UpdateNewMessage -> {
                ingestIfSelected(update.message)
            }

            is TdApi.UpdateMessageContent -> {
                val tdClient = client ?: return
                val refreshed = runCatching {
                    tdClient.send(TdApi.GetMessage(update.chatId, update.messageId))
                }.getOrNull()
                if (refreshed is TdApi.Message) ingestIfSelected(refreshed)
            }
        }
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

    private fun requireClient(): TdClient =
        checkNotNull(client) { "Telegram client is not started" }

    private fun requireAuthState(expected: TelegramAuthState) {
        check(authState.value == expected) {
            "Authentication action is not valid in state ${authState.value}"
        }
    }
}

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

private fun TdApi.AuthorizationState.toLightAlertState(): TelegramAuthState = when (this) {
    is TdApi.AuthorizationStateWaitTdlibParameters,
    is TdApi.AuthorizationStateWaitEncryptionKey -> TelegramAuthState.INITIALIZING
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
