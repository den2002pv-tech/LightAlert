package app.lightalert.presentation.home

data class HomeUiState(
    val isScheduleConfigured: Boolean = false,
    val appVersion: String = "0.1.0",
    val todayQueues: List<TodayQueueUi> = emptyList(),
)
