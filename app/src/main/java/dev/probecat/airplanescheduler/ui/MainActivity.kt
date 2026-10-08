package dev.probecat.airplanescheduler.ui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.probecat.airplanescheduler.ui.schedules.SchedulesScreen
import dev.probecat.airplanescheduler.ui.theme.AppTheme
import dev.probecat.airplanescheduler.ui.theme.isDark

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            SystemBars(isDark(settings.theme))
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshShizuku() }
            AppTheme(settings.theme, settings.dynamicColor) {
                SchedulesScreen(viewModel)
            }
        }
    }

    // The in-app theme can differ from the system's, so the bar icons follow the app.
    @Composable
    private fun SystemBars(dark: Boolean) {
        DisposableEffect(dark) {
            val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
            enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            onDispose {}
        }
    }
}
