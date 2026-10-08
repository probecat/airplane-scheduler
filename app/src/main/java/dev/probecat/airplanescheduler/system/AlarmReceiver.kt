package dev.probecat.airplanescheduler.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.LocalDateTime

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AlarmScheduler.scheduleAll(context)
        when (intent.action) {
            AlarmScheduler.CHANGE -> AirplaneController.onAlarm(context, goAsync()::finish)
            AlarmScheduler.REMIND -> ShizukuReminder.check(context, goAsync()::finish)
            // A start or end missed before boot is skipped, so schedules that ran once are over.
            Intent.ACTION_BOOT_COMPLETED -> ScheduleUpdater.expire(context, LocalDateTime.now())
            // App updates and clock changes only reset the alarms. A start or end missed in the
            // meantime is skipped.
        }
    }
}
