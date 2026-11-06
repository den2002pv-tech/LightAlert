package app.lightalert.presentation.telegram

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.lightalert.data.telegram.TdlibTelegramClient
import app.lightalert.data.telegram.TelegramAuthState
import app.lightalert.data.telegram.TelegramChatSelectionStore
import app.lightalert.data.telegram.TelegramChatSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TelegramChannelUiState(
    val chats: List<TelegramChatSummary> = emptyList(),
    val selectedChatIds: Set<Long> = emptySet(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val authState: TelegramAuthState = TelegramAuthState.NOT_CONFIGURED,
) {
    val filteredChats: List<TelegramChatSummary>
        get() = if (searchQuery.isBlank()) chats else chats.filter {
            it.title.contains(searchQuery, ignoreCase = true)
        }
}

class TelegramChannelViewModel(
    private val telegramClient: TdlibTelegramClient,
    private val selectionStore: TelegramChatSelectionStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TelegramChannelUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            selectionStore.selectedChatIds.collect { ids ->
                _uiState.update { it.copy(selectedChatIds = ids) }
                telegramClient.setSelectedChatIds(ids)
            }
        }
        viewModelScope.launch {
            telegramClient.authState.collect { state ->
                _uiState.update { it.copy(authState = state) }
                if (state == TelegramAuthState.READY && _uiState.value.chats.isEmpty()) {
                    refresh()
                }
            }
        }
    }

    fun setSearchQuery(value: String) {
        _uiState.update { it.copy(searchQuery = value) }
    }

    fun toggleChat(chatId: Long) {
        _uiState.update { state ->
            val updated = state.selectedChatIds.toMutableSet().apply {
                if (!add(chatId)) remove(chatId)
            }
            state.copy(selectedChatIds = updated)
        }
    }

    fun refresh() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val chats = telegramClient.loadChats()
                _uiState.update { it.copy(chats = chats) }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(errorMessage = exception.message ?: "Не удалось загрузить список чатов.")
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun saveSelection() {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val ids = _uiState.value.selectedChatIds
                selectionStore.saveSelectedChatIds(ids)
                telegramClient.setSelectedChatIds(ids)
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(errorMessage = exception.message ?: "Не удалось сохранить выбор каналов.")
                }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}

class TelegramChannelViewModelFactory(
    private val telegramClient: TdlibTelegramClient,
    private val selectionStore: TelegramChatSelectionStore,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TelegramChannelViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TelegramChannelViewModel(telegramClient, selectionStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
