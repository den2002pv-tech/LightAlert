package app.lightalert.presentation.home

import androidx.lifecycle.ViewModel
import app.lightalert.BuildConfig
import app.lightalert.domain.model.ScheduleConfiguration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel : ViewModel() {
    private val scheduleConfiguration = ScheduleConfiguration()

    private val _uiState = MutableStateFlow(
        HomeUiState(
            isScheduleConfigured = scheduleConfiguration.isConfigured,
            appVersion = BuildConfig.VERSION_NAME,
        ),
    )

    val uiState = _uiState.asStateFlow()
}
