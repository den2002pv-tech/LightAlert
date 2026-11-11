package app.lightalert.presentation.telegram

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.lightalert.data.telegram.TdlibTelegramClient
import app.lightalert.data.telegram.TelegramAuthState
import app.lightalert.data.telegram.TelegramApiConfigStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TelegramAuthUiState(
    val authState: TelegramAuthState = TelegramAuthState.NOT_CONFIGURED,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
)

class TelegramAuthViewModel(
    private val telegramClient: TdlibTelegramClient,
    private val apiConfigStore: TelegramApiConfigStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TelegramAuthUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            telegramClient.authState.collect { authState ->
                _uiState.update { it.copy(authState = authState) }
            }
        }
        telegramClient.start()
    }

    fun saveApiConfig(apiId: String, apiHash: String) {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                withContext(Dispatchers.IO) {
                    apiConfigStore.save(apiId.toInt(), apiHash.trim())
                    check(telegramClient.start()) {
                        "Не удалось запустить TDLib. Проверьте параметры и повторите попытку."
                    }
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(errorMessage = exception.message ?: "Не удалось сохранить настройки API.")
                }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    fun submitPhoneNumber(value: String) = submit {
        telegramClient.submitPhoneNumber(value)
    }

    fun submitCode(value: String) = submit {
        telegramClient.submitCode(value)
    }

    fun submitPassword(value: String) = submit {
        telegramClient.submitPassword(value)
    }

    fun retryConnection() {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(errorMessage = null) }
            try {
                check(telegramClient.start()) {
                    "Не удалось подключиться к Telegram. Повторите попытку."
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(errorMessage = exception.message ?: "Не удалось подключиться к Telegram.")
                }
            }
        }
    }

    private fun submit(action: suspend () -> Unit) {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                action()
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(errorMessage = exception.message ?: "Не удалось выполнить запрос.")
                }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }
}

class TelegramAuthViewModelFactory(
    private val telegramClient: TdlibTelegramClient,
    private val apiConfigStore: TelegramApiConfigStore,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TelegramAuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TelegramAuthViewModel(telegramClient, apiConfigStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
