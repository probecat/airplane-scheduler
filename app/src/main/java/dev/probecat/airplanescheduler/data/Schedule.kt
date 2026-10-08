package dev.probecat.airplanescheduler.data

import kotlinx.serialization.Serializable
import java.time.DayOfWeek

@Serializable
data class Schedule(
    // Stable; also picks the alarm request codes.
    val id: Long,
    val name: String = "",
    // Minutes since midnight. An end at or before the start ends the next day.
    val start: Int,
    val end: Int,
    // Days the window starts on.
    val days: Set<DayOfWeek> = EVERY_DAY,
    val enabled: Boolean = true,
    val disableWifi: Boolean = true,
    val enableWifi: Boolean = true,
) {
    companion object {
        val EVERY_DAY: Set<DayOfWeek> = DayOfWeek.entries.toSet()
    }
}
