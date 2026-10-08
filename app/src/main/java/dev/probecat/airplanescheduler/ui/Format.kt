package dev.probecat.airplanescheduler.ui

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import dev.probecat.airplanescheduler.data.Schedule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

// Time and day labels that follow the system 24-hour setting and locale.
class Format(val is24Hour: Boolean, val locale: Locale) {
    private val clock = DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm", locale)
    private val marker = DateTimeFormatter.ofPattern("a", locale)

    // Days in the locale's week order, starting on Sunday or Monday.
    val week: List<DayOfWeek> = WeekFields.of(locale).firstDayOfWeek.let { first ->
        List(7) { first.plus(it.toLong()) }
    }

    fun time(minute: Int): String = time(minute, suffixSize = TextUnit.Unspecified).text

    // "11:00 PM" with a smaller marker.
    fun time(minute: Int, suffixSize: TextUnit): AnnotatedString {
        val time = LocalTime.of(minute / 60, minute % 60)
        return buildAnnotatedString {
            append(clock.format(time))
            if (!is24Hour) {
                append(NBSP)
                withStyle(SpanStyle(fontSize = suffixSize)) { append(marker.format(time)) }
            }
        }
    }

    fun window(schedule: Schedule): String = "${time(schedule.start)} – ${time(schedule.end)}"

    fun window(schedule: Schedule, suffixSize: TextUnit): AnnotatedString = buildAnnotatedString {
        append(time(schedule.start, suffixSize))
        append(" – ")
        append(time(schedule.end, suffixSize))
    }

    // A single run of three or more days as a range, otherwise each day.
    fun days(days: Set<DayOfWeek>): String {
        if (days == Schedule.EVERY_DAY) return "Every day"
        val ordered = week.filter { it in days }
        if (ordered.isEmpty()) return "Once"
        val run = ordered.size >= 3 && week.indexOf(ordered.last()) - week.indexOf(ordered.first()) == ordered.size - 1
        return if (run) "${short(ordered.first())}–${short(ordered.last())}" else ordered.joinToString(" ", transform = ::short)
    }

    // "Once, today" for a schedule that runs once, otherwise its days.
    fun repeats(schedule: Schedule, today: LocalDate): String =
        if (schedule.once) listOfNotNull("Once", onceDate(schedule, today)).joinToString(", ") else days(schedule.days)

    // When a schedule that runs once starts; nothing while it's off, since switching on picks a new date.
    private fun onceDate(schedule: Schedule, today: LocalDate): String? {
        val start = schedule.date?.takeIf { schedule.enabled } ?: return null
        return when (start) {
            today -> "today"
            today.plusDays(1) -> "tomorrow"
            today.minusDays(1) -> "yesterday"
            else -> null
        }
    }

    private fun short(day: DayOfWeek): String = day.getDisplayName(TextStyle.SHORT, locale)

    fun dayName(day: DayOfWeek): String = day.getDisplayName(TextStyle.FULL, locale)

    fun dayLetter(day: DayOfWeek): String = day.getDisplayName(TextStyle.NARROW, locale)

    fun label(schedule: Schedule, today: LocalDate): String {
        if (schedule.name.isNotBlank()) return "“${schedule.name}”"
        val days = when {
            schedule.once -> listOfNotNull("once", onceDate(schedule, today)).joinToString(", ")
            schedule.days == Schedule.EVERY_DAY -> "every day"
            else -> days(schedule.days)
        }
        return "${window(schedule)} ($days)"
    }

    fun duration(minutes: Int): String {
        val hours = minutes / 60
        val rest = minutes % 60
        return when {
            hours == 0 -> "$rest${NBSP}min"
            rest == 0 -> "$hours${NBSP}h"
            else -> "$hours${NBSP}h $rest${NBSP}min"
        }
    }

    companion object {
        val SMALL = 0.5.em

        // Keeps "11:00 PM" and "8 h" on one line.
        private const val NBSP = '\u00A0'
    }
}

@Composable
fun rememberFormat(): Format {
    // Reading the configuration recomposes when the locale changes.
    val locale = LocalConfiguration.current.locales[0]
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    return remember(is24Hour, locale) { Format(is24Hour, locale) }
}
