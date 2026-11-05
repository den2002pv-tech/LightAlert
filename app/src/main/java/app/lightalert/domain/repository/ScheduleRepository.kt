package app.lightalert.domain.repository

import app.lightalert.domain.model.ScheduleConfiguration
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    val schedule: Flow<ScheduleConfiguration>
    suspend fun save(configuration: ScheduleConfiguration)
    suspend fun clear()
}
