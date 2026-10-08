package dev.probecat.airplanescheduler.system

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dev.probecat.airplanescheduler.core.ScheduleResolver
import java.time.ZonedDateTime

object AlarmScheduler {
    const val CHANGE = "dev.probecat.airplanescheduler.CHANGE"
    const val REMIND = "dev.probecat.airplanescheduler.REMIND"
    const val REMIND_BEFORE_MINUTES = 60L
    private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    // One exact alarm for the next start or end of any schedule, plus the reminder.
    // Recomputed in local time on every alarm so clock and time-zone changes stay correct.
    fun scheduleAll(context: Context) {
        val app = App.from(context)
        val schedules = app.schedules.all
        val now = ZonedDateTime.now()
        set(context, CHANGE, ScheduleResolver.nextChange(schedules, now))
        val reminder = if (app.settings.current.remindShizuku) {
            ScheduleResolver.nextReminder(schedules, now, REMIND_BEFORE_MINUTES)
        } else {
            null
        }
        set(context, REMIND, reminder)
    }

    // v1 used fixed request codes and a receiver in the root package.
    fun cancelV1(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        listOf("START" to 1, "END" to 2, "REMIND" to 3).forEach { (action, code) ->
            val intent = Intent("dev.probecat.airplanescheduler.$action")
                .setClassName(context, "dev.probecat.airplanescheduler.AlarmReceiver")
            alarms.cancel(PendingIntent.getBroadcast(context, code, intent, FLAGS))
        }
    }

    // Setting the same intent again replaces its alarm; no time cancels it.
    @SuppressLint("MissingPermission") // USE_EXACT_ALARM is granted at install.
    private fun set(context: Context, action: String, time: ZonedDateTime?) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, AlarmReceiver::class.java).setAction(action)
        val pending = PendingIntent.getBroadcast(context, 0, intent, FLAGS)
        if (time == null) {
            alarms.cancel(pending)
        } else {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time.toInstant().toEpochMilli(), pending)
        }
    }
}
