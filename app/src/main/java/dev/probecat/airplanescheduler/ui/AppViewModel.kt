package dev.probecat.airplanescheduler.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.probecat.airplanescheduler.core.ScheduleResolver
import dev.probecat.airplanescheduler.data.Schedule
import dev.probecat.airplanescheduler.data.Settings
import dev.probecat.airplanescheduler.system.AlarmScheduler
import dev.probecat.airplanescheduler.system.App
import dev.probecat.airplanescheduler.system.ScheduleUpdater
import dev.probecat.airplanescheduler.system.ShizukuState
import dev.probecat.airplanescheduler.system.ShizukuStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as App

    val schedules: StateFlow<List<Schedule>> = app.schedules.schedules
        .map(::sorted)
        .stateIn(viewModelScope, SharingStarted.Eagerly, sorted(app.schedules.all))

    val settings: StateFlow<Settings> = app.settings.settings

    val shizuku: StateFlow<ShizukuState> = app.shizuku

    // Ticks on each minute so "Active now" follows the clock.
    val now: StateFlow<LocalDateTime> = flow {
        while (true) {
            val now = LocalDateTime.now()
            emit(now)
            delay(60_000L - now.second * 1000L - now.nano / 1_000_000L + 50)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalDateTime.now())

    // Asks for Shizuku access once per visit when Shizuku runs without it.
    init {
        viewModelScope.launch {
            shizuku.first { it == ShizukuState.PERMISSION_NEEDED }
            ShizukuStatus.request()
        }
    }

    fun refreshShizuku() = app.refreshShizuku()

    fun requestShizuku(): Boolean = ShizukuStatus.request()

    fun newSchedule(): Schedule = Schedule(id = app.schedules.nextId(), start = 23 * 60, end = 7 * 60)

    fun isNew(schedule: Schedule): Boolean = app.schedules.get(schedule.id) == null

    // Only enabled schedules are checked, so a disabled one may overlap anything.
    fun conflict(schedule: Schedule): Schedule? =
        if (schedule.enabled) ScheduleResolver.conflict(schedule, app.schedules.all) else null

    // Returns the schedule in the way instead of switching on.
    fun setEnabled(schedule: Schedule, enabled: Boolean): Schedule? = save(schedule.copy(enabled = enabled))

    fun save(schedule: Schedule): Schedule? {
        conflict(schedule)?.let { return it }
        val all = app.schedules.all
        commit(if (all.any { it.id == schedule.id }) all.map { if (it.id == schedule.id) schedule else it } else all + schedule)
        return null
    }

    fun delete(schedule: Schedule) {
        commit(app.schedules.all.filterNot { it.id == schedule.id })
    }

    // Undo of a delete. If something took its place meanwhile, it comes back switched off.
    fun restore(schedule: Schedule) {
        val id = if (app.schedules.get(schedule.id) == null) schedule.id else app.schedules.nextId()
        val restored = schedule.copy(id = id).let { if (conflict(it) != null) it.copy(enabled = false) else it }
        commit(app.schedules.all + restored)
    }

    fun updateSettings(transform: (Settings) -> Settings) {
        val before = app.settings.current
        app.settings.update(transform)
        if (before.remindShizuku != app.settings.current.remindShizuku) {
            AlarmScheduler.scheduleAll(app)
        }
    }

    private fun commit(schedules: List<Schedule>) {
        if (!ScheduleUpdater.update(app, schedules)) {
            ShizukuStatus.request()
        }
    }

    private fun sorted(schedules: List<Schedule>) = schedules.sortedWith(compareBy({ it.start }, { it.end }, { it.id }))
}
