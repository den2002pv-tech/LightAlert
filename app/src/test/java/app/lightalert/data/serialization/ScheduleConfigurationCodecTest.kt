package app.lightalert.data.serialization

import app.lightalert.domain.model.OutageInterval
import app.lightalert.domain.model.ScheduleConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class ScheduleConfigurationCodecTest {
    private val codec = ScheduleConfigurationCodec()

    @Test
    fun encodeAndDecodePreservesIntervals() {
        val original = ScheduleConfiguration(
            intervals = listOf(
                OutageInterval(LocalTime.of(8, 15), LocalTime.of(12, 30)),
                OutageInterval(LocalTime.of(23, 0), LocalTime.of(1, 45)),
            ),
        )

        assertEquals(original, codec.decode(codec.encode(original)))
    }

    @Test
    fun invalidJsonReturnsEmptyConfiguration() {
        val decoded = codec.decode("{not-valid-json")

        assertFalse(decoded.isConfigured)
    }

    @Test
    fun outOfRangeIntervalsAreIgnored() {
        val decoded = codec.decode(
            """[{"startMinute":-1,"endMinute":120},{"startMinute":480,"endMinute":720}]""",
        )

        assertEquals(1, decoded.intervals.size)
        assertTrue(decoded.isConfigured)
        assertEquals(LocalTime.of(8, 0), decoded.intervals.single().start)
    }
}
