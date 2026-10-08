package dev.probecat.airplanescheduler.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class ScheduleRepositoryTest {
    @Test
    fun daysAreStoredByName() {
        val json = ScheduleRepository.encode(listOf(Schedule(id = 1, start = 0, end = 60, days = setOf(DayOfWeek.MONDAY))))
        assertTrue(json, json.contains("\"days\":[\"MONDAY\"]"))
    }

    @Test
    fun missingFieldsUseDefaults() {
        assertEquals(
            listOf(Schedule(id = 3, start = 60, end = 120)),
            ScheduleRepository.decode("""[{"id":3,"start":60,"end":120,"future":1}]"""),
        )
    }

    @Test
    fun unreadableValueIsEmpty() {
        assertEquals(emptyList<Schedule>(), ScheduleRepository.decode(null))
        assertEquals(emptyList<Schedule>(), ScheduleRepository.decode("not json"))
    }
}
