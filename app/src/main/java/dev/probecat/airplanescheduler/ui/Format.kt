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

    // "11:00 PM" with a smaller marker, as Clock shows it.
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

    fun days(days: Set<DayOfWeek>): String = when (days) {
        Schedule.EVERY_DAY -> "Every day"
        WEEKDAYS -> "Weekdays"
        WEEKEND -> "Weekends"
        emptySet<DayOfWeek>() -> "No days"
        else -> week.filter { it in days }.joinToString(", ") { it.getDisplayName(TextStyle.SHORT, locale) }
    }

    fun dayName(day: DayOfWeek): String = day.getDisplayName(TextStyle.FULL, locale)

    fun dayLetter(day: DayOfWeek): String = day.getDisplayName(TextStyle.NARROW, locale)

    fun label(schedule: Schedule): String =
        if (schedule.name.isBlank()) "${window(schedule)} (${days(schedule.days).lowercase(locale)})" else "“${schedule.name}”"

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
        private val WEEKDAYS = setOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
        )
        private val WEEKEND = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    }
}

@Composable
fun rememberFormat(): Format {
    // Reading the configuration recomposes when the locale changes.
    val locale = LocalConfiguration.current.locales[0]
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    return remember(is24Hour, locale) { Format(is24Hour, locale) }
}
