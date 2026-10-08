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
import java.time.LocalDate
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
    fun everyDayAndOnceAreNamed() {
        assertEquals("Every day", us.days(Schedule.EVERY_DAY))
        assertEquals("Once", us.days(emptySet()))
    }

    @Test
    fun onceShowsItsDateWhileOn() {
        val today = LocalDate.of(2026, 10, 9)
        val once = Schedule(id = 1, start = 0, end = 60, days = emptySet(), date = today)
        assertEquals("Once, today", us.repeats(once, today))
        assertEquals("Once, tomorrow", us.repeats(once.copy(date = today.plusDays(1)), today))
        assertEquals("Once, yesterday", us.repeats(once.copy(date = today.minusDays(1)), today))
        assertEquals("Once", us.repeats(once.copy(enabled = false), today))
        assertEquals("12:00\u00A0AM – 1:00\u00A0AM (once, today)", us.label(once, today))
    }
}
