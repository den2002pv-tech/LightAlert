package app.lightalert.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.lightalert.BuildConfig
import app.lightalert.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val scheduleRepository: ScheduleRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        HomeUiState(appVersion = BuildConfig.VERSION_NAME),
    )

    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            scheduleRepository.schedule.collect { configuration ->
                _uiState.update { current ->
                    current.copy(isScheduleConfigured = configuration.isConfigured)
                }
            }
        }
    }
}

class HomeViewModelFactory(
    private val scheduleRepository: ScheduleRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(scheduleRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
