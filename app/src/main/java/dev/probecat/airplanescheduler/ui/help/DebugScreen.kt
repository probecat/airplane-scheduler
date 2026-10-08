package dev.probecat.airplanescheduler.ui.help

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.probecat.airplanescheduler.BuildConfig
import dev.probecat.airplanescheduler.R
import dev.probecat.airplanescheduler.core.ConnectivityPlan.Changes
import dev.probecat.airplanescheduler.core.ScheduleResolver
import dev.probecat.airplanescheduler.core.ScheduleTime
import dev.probecat.airplanescheduler.data.Schedule
import dev.probecat.airplanescheduler.data.Settings
import dev.probecat.airplanescheduler.system.AlarmScheduler
import dev.probecat.airplanescheduler.system.App
import dev.probecat.airplanescheduler.system.ShizukuState
import dev.probecat.airplanescheduler.system.notificationsAllowed
import dev.probecat.airplanescheduler.ui.AppViewModel
import dev.probecat.airplanescheduler.ui.Subpage
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DebugScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val shizuku by viewModel.shizuku.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()
    val details = details(context, schedules, settings, shizuku, now)
    Subpage(
        title = "Debug info",
        onBack = onBack,
        actions = {
            IconButton(onClick = {
                context.getSystemService(ClipboardManager::class.java)
                    .setPrimaryClip(ClipData.newPlainText("Airplane Scheduler debug info", details))
            }) {
                Icon(painterResource(R.drawable.ic_content_copy), contentDescription = "Copy")
            }
        },
    ) {
        SelectionContainer {
            Text(
                details,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
        }
    }
}

private val moment = DateTimeFormatter.ofPattern("EEE HH:mm", Locale.ROOT)

private fun details(
    context: Context,
    schedules: List<Schedule>,
    settings: Settings,
    shizuku: ShizukuState,
    now: LocalDateTime,
): String {
    val app = App.from(context)
    val zoned = ZonedDateTime.now()
    val active = ScheduleResolver.activeSchedule(schedules, now)
    val reminder = ScheduleResolver.nextReminder(schedules, zoned, AlarmScheduler.REMIND_BEFORE_MINUTES)
    val started = app.settings.started
    return buildString {
        appendLine("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("Package: ${context.packageName}")
        appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("Time zone: ${zoned.zone}")
        appendLine(
            "Shizuku: " + when (shizuku) {
                ShizukuState.READY -> "ready"
                ShizukuState.OFFLINE -> "offline"
                ShizukuState.PERMISSION_NEEDED -> "permission required"
            },
        )
        appendLine("Notifications: " + if (notificationsAllowed(context)) "allowed" else "not allowed")
        appendLine("Theme: ${settings.theme.label}, dynamic color ${if (settings.dynamicColor) "on" else "off"}")
        appendLine(
            "Shizuku reminder: " + if (settings.remindShizuku) reminder?.let { "next ${moment.format(it)}" } ?: "on" else "off",
        )
        appendLine(
            "Started window: " + (started?.let { "${moment.format(it.window)}, changed ${changed(it.changes)}" } ?: "none"),
        )
        appendLine("Active now: ${active?.let { "#${it.id}" } ?: "none"}")
        appendLine()
        append("Schedules: ${schedules.size}")
        for (schedule in schedules) {
            appendLine()
            appendLine()
            append(scheduleDetails(schedule, zoned))
        }
    }
}

private fun changed(changes: Changes): String =
    listOfNotNull("airplane mode".takeIf { changes.airplane }, "Wi-Fi".takeIf { changes.wifi })
        .joinToString(" and ").ifEmpty { "nothing" }

private fun scheduleDetails(schedule: Schedule, now: ZonedDateTime): String = buildString {
    val name = if (schedule.name.isBlank()) "" else " “${schedule.name}”"
    appendLine("#${schedule.id}$name: ${if (schedule.enabled) "on" else "off"}")
    appendLine("  Window: ${ScheduleTime.format(schedule.start)}–${ScheduleTime.format(schedule.end)}")
    appendLine(
        "  Days: " + when {
            schedule.once -> "once, ${schedule.date ?: "no date"}"
            schedule.days == Schedule.EVERY_DAY -> "every day"
            else -> schedule.days.sorted().joinToString(" ") { it.name.take(3).lowercase() }
        },
    )
    appendLine("  Wi-Fi at start: ${if (schedule.disableWifi) "disable" else "leave unchanged"}")
    append(
        "  Wi-Fi at end: " + when {
            !schedule.enableWifi -> "leave unchanged"
            schedule.disableWifi -> "turn back on"
            else -> "turn back on (no effect: Wi-Fi at start is off)"
        },
    )
    if (schedule.enabled) {
        appendLine()
        appendLine("  Next start: ${ScheduleResolver.nextStart(schedule, now)?.let(moment::format) ?: "none"}")
        append("  Next end: ${ScheduleResolver.nextEnd(schedule, now)?.let(moment::format) ?: "none"}")
    }
}
