package dev.probecat.airplanescheduler.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V1MigrationTest {
    @Test
    fun savedScheduleBecomesFirstSchedule() {
        val result = V1Migration.migrate(
            mapOf(
                "saved" to true,
                "start" to 22 * 60 + 30,
                "end" to 6 * 60,
                "disableWifi" to false,
                "enableWifi" to true,
                "remindShizuku" to true,
            ),
        )
        assertEquals(
            listOf(
                Schedule(
                    id = 1,
                    start = 22 * 60 + 30,
                    end = 6 * 60,
                    days = Schedule.EVERY_DAY,
                    enabled = true,
                    disableWifi = false,
                    enableWifi = true,
                ),
            ),
            result.schedules,
        )
        assertTrue(result.remindShizuku)
    }

    @Test
    fun removedScheduleIsNotMigrated() {
        val result = V1Migration.migrate(
            mapOf("saved" to false, "start" to 60, "end" to 120, "remindShizuku" to true),
        )
        assertTrue(result.schedules.isEmpty())
        assertTrue(result.remindShizuku)
    }

    @Test
    fun freshInstallHasNothingToMigrate() {
        val result = V1Migration.migrate(emptyMap<String, Any>())
        assertTrue(result.schedules.isEmpty())
        assertFalse(result.remindShizuku)
    }

    @Test
    fun missingValuesUseV1Defaults() {
        val schedule = V1Migration.migrate(mapOf("saved" to true)).schedules.single()
        assertEquals(23 * 60, schedule.start)
        assertEquals(7 * 60, schedule.end)
        assertTrue(schedule.disableWifi)
        assertTrue(schedule.enableWifi)
    }
}
