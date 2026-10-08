package dev.probecat.airplanescheduler.data

import android.content.Context
import androidx.core.content.edit
import dev.probecat.airplanescheduler.core.Boundary
import dev.probecat.airplanescheduler.core.ConnectivityPlan.Changes
import java.time.LocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val remindShizuku: Boolean = false,
    val hideShizukuReady: Boolean = false,
)

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())

    val settings: StateFlow<Settings> = state.asStateFlow()

    val current: Settings get() = state.value

    fun update(transform: (Settings) -> Settings) {
        val settings = transform(state.value)
        state.value = settings
        prefs.edit {
            putString("theme", settings.theme.name)
            putBoolean("dynamicColor", settings.dynamicColor)
            putBoolean("remindShizuku", settings.remindShizuku)
            putBoolean("hideShizukuReady", settings.hideShizukuReady)
        }
    }

    var started: Boundary.Started?
        get() = prefs.getString("startedWindow", null)?.let { window ->
            Boundary.Started(LocalDateTime.parse(window), Changes.of(prefs.getInt("startedChanges", 0)))
        }
        set(value) {
            prefs.edit {
                if (value == null) {
                    remove("startedWindow")
                    remove("startedChanges")
                } else {
                    putString("startedWindow", value.window.toString())
                    putInt("startedChanges", value.changes.bits)
                }
            }
        }

    val migrated: Boolean get() = prefs.getBoolean("migrated", false)

    fun migrate(result: V1Migration.Result) {
        prefs.edit {
            putBoolean("remindShizuku", result.remindShizuku)
            putBoolean("migrated", true)
        }
        state.value = read()
    }

    private fun read() = Settings(
        theme = prefs.getString("theme", null)
            ?.let { name -> ThemeMode.entries.find { it.name == name } } ?: ThemeMode.SYSTEM,
        dynamicColor = prefs.getBoolean("dynamicColor", true),
        remindShizuku = prefs.getBoolean("remindShizuku", false),
        hideShizukuReady = prefs.getBoolean("hideShizukuReady", false),
    )

    private companion object {
        const val PREFS = "settings"
    }
}
