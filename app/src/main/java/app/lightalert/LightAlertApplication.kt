package app.lightalert

import android.app.Application
import app.lightalert.data.repository.PreferencesScheduleRepository
import app.lightalert.domain.repository.ScheduleRepository

class LightAlertApplication : Application() {
    val scheduleRepository: ScheduleRepository by lazy {
        PreferencesScheduleRepository(applicationContext)
    }
}
