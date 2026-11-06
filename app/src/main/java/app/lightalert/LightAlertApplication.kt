package app.lightalert

import android.app.Application
import app.lightalert.data.local.db.LightAlertDatabaseProvider
import app.lightalert.data.local.db.RoomScheduleHistoryStore
import app.lightalert.data.ingestion.ScheduleMessageIngestionService
import app.lightalert.data.local.db.ScheduleHistoryStore
import app.lightalert.data.telegram.TdlibTelegramClient
import app.lightalert.data.telegram.TelegramApiConfigStore
import app.lightalert.data.telegram.TelegramChatSelectionStore
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

    val scheduleMessageIngestionService: ScheduleMessageIngestionService by lazy {
        ScheduleMessageIngestionService(scheduleHistoryStore)
    }

    val telegramChatSelectionStore: TelegramChatSelectionStore by lazy {
        TelegramChatSelectionStore(applicationContext)
    }

    val telegramApiConfigStore: TelegramApiConfigStore by lazy {
        TelegramApiConfigStore(applicationContext)
    }

    val telegramClient: TdlibTelegramClient by lazy {
        TdlibTelegramClient(
            applicationContext,
            scheduleMessageIngestionService,
            telegramApiConfigStore,
        )
    }
}
