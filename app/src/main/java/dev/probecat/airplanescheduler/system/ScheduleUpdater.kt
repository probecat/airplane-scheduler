package dev.probecat.airplanescheduler.system

import android.content.Context
import dev.probecat.airplanescheduler.core.Boundary
import dev.probecat.airplanescheduler.data.Schedule
import java.time.LocalDateTime

object ScheduleUpdater {
    // Saves the list and resets the alarms. A window that the edit switches on or off starts or
    // ends at once. Returns false when that needs Shizuku but it isn't ready.
    fun update(context: Context, schedules: List<Schedule>): Boolean {
        val app = App.from(context)
        val step = Boundary.atEdit(app.schedules.all, schedules, LocalDateTime.now(), app.settings.started)
        app.schedules.replaceAll(schedules)
        AlarmScheduler.scheduleAll(app)
        AirplaneController.run(app, step) {}
        val needsShizuku = step is Boundary.Step.Start || (step is Boundary.Step.End && !step.changes.isEmpty)
        return !needsShizuku || ShizukuStatus.isReady()
    }
}
