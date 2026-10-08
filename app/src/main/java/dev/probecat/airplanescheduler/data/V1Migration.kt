package dev.probecat.airplanescheduler.data

import android.content.Context

// v1 kept one schedule in the "schedule" prefs. Its values become the first v2 schedule.
object V1Migration {
    private const val PREFS = "schedule"

    data class Result(val schedules: List<Schedule>, val remindShizuku: Boolean)

    fun migrate(v1: Map<String, *>): Result {
        fun flag(key: String, default: Boolean) = v1[key] as? Boolean ?: default
        val schedules = if (flag("saved", false)) {
            listOf(
                Schedule(
                    id = 1,
                    start = v1["start"] as? Int ?: (23 * 60),
                    end = v1["end"] as? Int ?: (7 * 60),
                    disableWifi = flag("disableWifi", true),
                    enableWifi = flag("enableWifi", true),
                ),
            )
        } else {
            emptyList()
        }
        return Result(
            schedules = schedules,
            remindShizuku = flag("remindShizuku", false),
        )
    }

    // Runs once; the v1 prefs stay in place but are never read again.
    fun run(context: Context, schedules: ScheduleRepository, settings: SettingsRepository): Boolean {
        if (settings.migrated) return false
        val result = migrate(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).all)
        if (schedules.all.isEmpty()) {
            schedules.replaceAll(result.schedules)
        }
        settings.migrate(result)
        return true
    }
}
