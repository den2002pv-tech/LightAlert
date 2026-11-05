package app.lightalert.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import app.lightalert.data.local.scheduleDataStore
import app.lightalert.data.serialization.ScheduleConfigurationCodec
import app.lightalert.domain.model.ScheduleConfiguration
import app.lightalert.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class PreferencesScheduleRepository(
    context: Context,
    private val codec: ScheduleConfigurationCodec = ScheduleConfigurationCodec(),
) : ScheduleRepository {
    private val dataStore = context.scheduleDataStore()

    override val schedule: Flow<ScheduleConfiguration> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            codec.decode(preferences[SCHEDULE_CONFIGURATION_KEY])
        }

    override suspend fun save(configuration: ScheduleConfiguration) {
        dataStore.edit { preferences ->
            preferences[SCHEDULE_CONFIGURATION_KEY] = codec.encode(configuration)
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(SCHEDULE_CONFIGURATION_KEY)
        }
    }

    private companion object {
        val SCHEDULE_CONFIGURATION_KEY = stringPreferencesKey("schedule_configuration")
    }
}
