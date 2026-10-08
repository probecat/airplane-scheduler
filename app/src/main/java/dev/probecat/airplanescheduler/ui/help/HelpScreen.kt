package dev.probecat.airplanescheduler.ui.help

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.probecat.airplanescheduler.ui.Format
import dev.probecat.airplanescheduler.ui.SectionHeader
import dev.probecat.airplanescheduler.ui.Subpage
import dev.probecat.airplanescheduler.ui.rememberFormat

private fun sections(format: Format) = listOf(
    "How it works" to
        "Each schedule turns on airplane mode at its start time and turns it off at its end time. At the " +
        "end, the app undoes only what it changed at the start. If airplane mode was already on at the " +
        "start, it stays on after the end. If you turn off airplane mode during a schedule, the app does not " +
        "turn it on again.",
    "Wi-Fi" to
        "“Turn off Wi-Fi at start” turns off Wi-Fi before airplane mode turns on. If Wi-Fi is already off, " +
        "the app does not change it. “Turn Wi-Fi back on at end” turns on Wi-Fi after airplane mode turns " +
        "off, but only if the app turned it off at the start.",
    "What Shizuku is" to
        "Shizuku is an app that lets other apps run system commands without root. Android does not let " +
        "regular apps turn airplane mode on or off, so Airplane Scheduler uses Shizuku to do it.",
    "After a reboot" to
        "Shizuku stops when the phone restarts. Open Shizuku and start it again. To get a warning, turn on " +
        "“Remind me if Shizuku is off” in Settings. If Shizuku is not ready one hour before a schedule " +
        "starts, you get a notification.",
    "If Shizuku is not running at a start or end" to
        "Nothing changes at that time, and the app does not try again later. If airplane mode stays on " +
        "after a missed end, turn it off yourself. To start a schedule late, start Shizuku, then switch the " +
        "schedule off and on again.",
    "Days and overlaps" to
        "The days of a schedule are the days on which it starts. For example, a Friday schedule from " +
        "${format.time(23 * 60)} to ${format.time(7 * 60)} ends on Saturday morning. Two schedules that are " +
        "switched on cannot overlap. If one schedule ends at the same time that another starts, they work as " +
        "one schedule. Airplane mode stays on between them. Wi-Fi follows “Turn off Wi-Fi at start” of the " +
        "first schedule and “Turn Wi-Fi back on at end” of the last schedule.",
    "Changing an active schedule" to
        "If you switch off or delete a schedule while it is active, it ends immediately. The app undoes what " +
        "it changed at the start, as at a normal end. If you switch on or add a schedule that covers the " +
        "current time, it starts immediately. Changes to the times work the same way.",
)

@Composable
fun HelpScreen(onBack: () -> Unit) {
    Subpage("Help", onBack) {
        sections(rememberFormat()).forEach { (title, text) ->
            SectionHeader(title)
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
    }
}
