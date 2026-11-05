package app.lightalert.domain.model

/**
 * A locally available schedule configuration.
 *
 * An empty interval list means that the user has not configured a schedule yet.
 */
data class ScheduleConfiguration(
    val intervals: List<OutageInterval> = emptyList(),
) {
    val isConfigured: Boolean
        get() = intervals.isNotEmpty()
}
