package dev.probecat.airplanescheduler.ui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.probecat.airplanescheduler.ui.help.DebugScreen
import dev.probecat.airplanescheduler.ui.help.HelpScreen
import dev.probecat.airplanescheduler.ui.schedules.SchedulesScreen
import dev.probecat.airplanescheduler.ui.settings.SettingsScreen
import dev.probecat.airplanescheduler.ui.theme.AppTheme
import dev.probecat.airplanescheduler.ui.theme.isDark
import kotlinx.serialization.Serializable

@Serializable
private object SchedulesRoute

@Serializable
private object SettingsRoute

@Serializable
private object HelpRoute

@Serializable
private object DebugRoute

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            SystemBars(isDark(settings.theme))
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshShizuku() }
            AppTheme(settings.theme, settings.dynamicColor) {
                val navController = rememberNavController()
                NavHost(
                    navController,
                    startDestination = SchedulesRoute,
                    // No animation, as in Droid-ify. Pop transitions default to these; predictive
                    // back's don't.
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    predictivePopEnterTransition = { EnterTransition.None },
                    predictivePopExitTransition = { ExitTransition.None },
                ) {
                    composable<SchedulesRoute> {
                        SchedulesScreen(
                            viewModel,
                            onSettings = { navController.navigate(SettingsRoute) },
                            onHelp = { navController.navigate(HelpRoute) },
                            onDebug = { navController.navigate(DebugRoute) },
                        )
                    }
                    composable<SettingsRoute> { SettingsScreen(viewModel, onBack = navController::navigateUp) }
                    composable<HelpRoute> { HelpScreen(onBack = navController::navigateUp) }
                    composable<DebugRoute> { DebugScreen(viewModel, onBack = navController::navigateUp) }
                }
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
