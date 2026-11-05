package app.lightalert.data.serialization

import app.lightalert.domain.model.OutageInterval
import app.lightalert.domain.model.ScheduleConfiguration
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import java.time.LocalTime

internal class ScheduleConfigurationCodec(
    private val gson: Gson = Gson(),
) {
    private val listType = object : TypeToken<List<StoredInterval>>() {}.type

    fun encode(configuration: ScheduleConfiguration): String {
        val storedIntervals = configuration.intervals.map { interval ->
            StoredInterval(
                startMinute = interval.start.toSecondOfDay() / SECONDS_PER_MINUTE,
                endMinute = interval.end.toSecondOfDay() / SECONDS_PER_MINUTE,
            )
        }
        return gson.toJson(storedIntervals)
    }

    fun decode(json: String?): ScheduleConfiguration {
        if (json.isNullOrBlank()) return ScheduleConfiguration()

        return try {
            val storedIntervals: List<StoredInterval> =
                gson.fromJson<List<StoredInterval>>(json, listType).orEmpty()

            ScheduleConfiguration(
                intervals = storedIntervals.mapNotNull { stored ->
                    if (
                        stored.startMinute !in MINUTES_PER_DAY ||
                        stored.endMinute !in MINUTES_PER_DAY
                    ) {
                        null
                    } else {
                        OutageInterval(
                            start = LocalTime.of(
                                stored.startMinute / MINUTES_PER_HOUR,
                                stored.startMinute % MINUTES_PER_HOUR,
                            ),
                            end = LocalTime.of(
                                stored.endMinute / MINUTES_PER_HOUR,
                                stored.endMinute % MINUTES_PER_HOUR,
                            ),
                        )
                    }
                },
            )
        } catch (_: JsonParseException) {
            ScheduleConfiguration()
        } catch (_: IllegalStateException) {
            ScheduleConfiguration()
        }
    }

    private data class StoredInterval(
        val startMinute: Int,
        val endMinute: Int,
    )

    private companion object {
        const val SECONDS_PER_MINUTE = 60
        const val MINUTES_PER_HOUR = 60
        val MINUTES_PER_DAY = 0 until 24 * MINUTES_PER_HOUR
    }
}
