package app.lightalert.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.lightalert.BuildConfig
import app.lightalert.data.local.db.ScheduleHistoryStore
import app.lightalert.domain.model.ParsedOutageInterval
import app.lightalert.domain.repository.ScheduleRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class HomeViewModel(
    scheduleRepository: ScheduleRepository,
    private val historyStore: ScheduleHistoryStore,
    private val gson: Gson = Gson(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        HomeUiState(appVersion = BuildConfig.VERSION_NAME),
    )
    val uiState = _uiState.asStateFlow()

    private val zoneId: ZoneId = ZoneId.of("Europe/Kyiv")
    private val intervalListType =
        object : TypeToken<List<ParsedOutageInterval>>() {}.type

    init {
        viewModelScope.launch {
            scheduleRepository.schedule.collect { configuration ->
                _uiState.update { it.copy(isScheduleConfigured = configuration.isConfigured) }
            }
        }
        viewModelScope.launch {
            val today = LocalDate.now(zoneId).toString()
            historyStore.observeEffectiveQueues(today).collect { rows ->
                val queues = rows.map { row ->
                    val intervals = runCatching {
                        gson.fromJson<List<ParsedOutageInterval>>(row.intervalsJson, intervalListType)
                    }.getOrNull().orEmpty()
                    TodayQueueUi(row.queueNumber, intervals)
                }
                _uiState.update { it.copy(todayQueues = queues) }
            }
        }
    }
}

class HomeViewModelFactory(
    private val scheduleRepository: ScheduleRepository,
    private val historyStore: ScheduleHistoryStore,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(scheduleRepository, historyStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
