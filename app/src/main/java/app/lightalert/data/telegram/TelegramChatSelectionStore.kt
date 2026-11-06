package app.lightalert.data.telegram

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Persists the user's explicit Telegram monitoring selection.
 *
 * Chat IDs are stored as strings because Preferences DataStore has no
 * LongSet preference type. No Telegram credentials or message contents
 * are stored here.
 */
class TelegramChatSelectionStore(context: Context) {
    private val dataStore: DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            produceFile = {
                context.applicationContext.preferencesDataStoreFile(FILE_NAME)
            },
        )

    val selectedChatIds: Flow<Set<Long>> = dataStore.data
        .map { preferences ->
            preferences[SELECTED_CHAT_IDS]
                .orEmpty()
                .mapNotNull(String::toLongOrNull)
                .toSet()
        }
        .distinctUntilChanged()

    suspend fun saveSelectedChatIds(chatIds: Set<Long>) {
        require(chatIds.none { it == 0L }) { "Telegram chat ID must not be zero" }
        dataStore.edit { preferences ->
            preferences[SELECTED_CHAT_IDS] = chatIds.map(Long::toString).toSet()
        }
    }

    companion object {
        private const val FILE_NAME = "telegram_chat_selection"
        private val SELECTED_CHAT_IDS = stringSetPreferencesKey("selected_chat_ids")
    }
}
