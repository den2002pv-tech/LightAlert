package app.lightalert

import android.app.Application
import app.lightalert.data.local.db.LightAlertDatabaseProvider
import app.lightalert.data.local.db.RoomScheduleHistoryStore
import app.lightalert.data.local.db.ScheduleHistoryStore
import app.lightalert.data.repository.PreferencesScheduleRepository
import app.lightalert.domain.repository.ScheduleRepository

class LightAlertApplication : Application() {
    val scheduleRepository: ScheduleRepository by lazy {
        PreferencesScheduleRepository(applicationContext)
    }

    val scheduleHistoryStore: ScheduleHistoryStore by lazy {
        RoomScheduleHistoryStore(
            LightAlertDatabaseProvider.get(applicationContext),
        )
    }
}
