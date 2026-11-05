package app.lightalert.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class ScheduleConfigurationTest {
    @Test
    fun emptyConfigurationIsNotConfigured() {
        val configuration = ScheduleConfiguration()

        assertFalse(configuration.isConfigured)
    }

    @Test
    fun configurationWithAnIntervalIsConfigured() {
        val configuration = ScheduleConfiguration(
            intervals = listOf(
                OutageInterval(
                    start = LocalTime.of(8, 0),
                    end = LocalTime.of(12, 30),
                ),
            ),
        )

        assertTrue(configuration.isConfigured)
    }
}
