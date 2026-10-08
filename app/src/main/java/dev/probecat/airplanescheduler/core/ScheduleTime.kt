package dev.probecat.airplanescheduler.core

import java.time.ZonedDateTime
import java.util.Locale

object ScheduleTime {
    const val DAY = 24 * 60

    fun isActive(start: Int, end: Int, minute: Int): Boolean {
        if (start == end) return false
        return if (start < end) minute in start until end else minute >= start || minute < end
    }

    // Same date as [day], at [minute] local time; a time in a DST gap moves to the next valid one.
    fun at(day: ZonedDateTime, minute: Int): ZonedDateTime =
        day.withHour(minute / 60).withMinute(minute % 60).withSecond(0).withNano(0)

    // Length of a window in minutes; an end at or before the start means the next day.
    fun duration(start: Int, end: Int): Int = Math.floorMod(end - start, DAY)

    fun format(minute: Int): String = String.format(Locale.ROOT, "%02d:%02d", minute / 60, minute % 60)
}
