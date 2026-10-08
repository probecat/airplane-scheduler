package dev.probecat.airplanescheduler.ui

import dev.probecat.airplanescheduler.data.Schedule
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.util.Locale

class FormatTest {
    private val us = Format(is24Hour = false, Locale.US)
    private val uk = Format(is24Hour = true, Locale.UK)

    @Test
    fun aSingleRunOfThreeOrMoreDaysIsARange() {
        assertEquals("Mon–Fri", us.days(setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)))
        assertEquals("Wed–Fri", us.days(setOf(WEDNESDAY, THURSDAY, FRIDAY)))
    }

    @Test
    fun otherDaysAreListedInWeekOrder() {
        assertEquals("Sun Mon Tue Thu Fri", us.days(setOf(FRIDAY, THURSDAY, TUESDAY, MONDAY, SUNDAY)))
        assertEquals("Sat Sun", uk.days(setOf(SATURDAY, SUNDAY)))
        assertEquals("Mon Tue", us.days(setOf(MONDAY, TUESDAY)))
    }

    @Test
    fun runsFollowTheLocalesFirstDayOfWeek() {
        assertEquals("Fri–Sun", uk.days(setOf(FRIDAY, SATURDAY, SUNDAY)))
        assertEquals("Sun Fri Sat", us.days(setOf(FRIDAY, SATURDAY, SUNDAY)))
    }

    @Test
    fun everyDayAndNoDaysAreNamed() {
        assertEquals("Every day", us.days(Schedule.EVERY_DAY))
        assertEquals("No days", us.days(emptySet()))
    }
}
