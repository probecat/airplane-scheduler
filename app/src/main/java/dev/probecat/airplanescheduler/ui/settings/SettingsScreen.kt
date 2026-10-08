package dev.probecat.airplanescheduler.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.probecat.airplanescheduler.BuildConfig
import dev.probecat.airplanescheduler.R
import dev.probecat.airplanescheduler.data.ThemeMode
import dev.probecat.airplanescheduler.system.ShizukuState
import dev.probecat.airplanescheduler.system.ShizukuStatus
import dev.probecat.airplanescheduler.system.notificationsAllowed
import dev.probecat.airplanescheduler.ui.AppViewModel
import dev.probecat.airplanescheduler.ui.ErrorText
import dev.probecat.airplanescheduler.ui.SectionHeader
import dev.probecat.airplanescheduler.ui.SettingRow
import dev.probecat.airplanescheduler.ui.ShizukuDialog
import dev.probecat.airplanescheduler.ui.Subpage
import dev.probecat.airplanescheduler.ui.SwitchRow

private const val SOURCE_URL = "https://github.com/probecat/airplane-scheduler"
private const val SHIZUKU_URL = "https://shizuku.rikka.app/"

@Composable
fun SettingsScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var notifications by remember { mutableStateOf(notificationsAllowed(context)) }
    var choosingTheme by rememberSaveable { mutableStateOf(false) }
    var reminderDenied by rememberSaveable { mutableStateOf(false) }
    val shizuku by viewModel.shizuku.collectAsStateWithLifecycle()
    val shizukuApp = context.packageManager.getLaunchIntentForPackage(ShizukuStatus.SHIZUKU_PACKAGE)
    var explaining by rememberSaveable { mutableStateOf<ShizukuState?>(null) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { notifications = notificationsAllowed(context) }
    // The reminder is useless without notifications, so a denial turns it back off.
    val requestNotifications =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notifications = notificationsAllowed(context)
            reminderDenied = !granted
            viewModel.updateSettings { it.copy(remindShizuku = granted) }
        }

    Subpage("Settings", onBack) {
        SectionHeader("Appearance")
        SettingRow(
            icon = R.drawable.ic_dark_mode,
            title = "Theme",
            text = settings.theme.label,
            onClick = { choosingTheme = true },
        )
        SwitchRow(
            icon = R.drawable.ic_palette,
            title = "Dynamic color",
            checked = settings.dynamicColor,
        ) { on -> viewModel.updateSettings { it.copy(dynamicColor = on) } }

        SectionHeader("Reminders")
        SwitchRow(
            icon = R.drawable.ic_notifications,
            title = "Remind me if Shizuku is off",
            checked = settings.remindShizuku,
        ) { on ->
            reminderDenied = false
            if (on && !notifications) {
                requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.updateSettings { it.copy(remindShizuku = on) }
            }
        }
        if (reminderDenied || (settings.remindShizuku && !notifications)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 72.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ErrorText("Notifications are off", Modifier.weight(1f))
                TextButton(onClick = { context.startActivity(notificationSettings(context)) }) { Text("Allow") }
            }
        }

        SectionHeader("Permissions")
        SettingRow(
            icon = when (shizuku) {
                ShizukuState.READY -> R.drawable.ic_check_circle
                ShizukuState.OFFLINE -> R.drawable.ic_error
                ShizukuState.PERMISSION_NEEDED -> R.drawable.ic_lock
            },
            title = "Shizuku",
            text = when (shizuku) {
                ShizukuState.READY -> "Ready"
                ShizukuState.OFFLINE -> "Not running"
                ShizukuState.PERMISSION_NEEDED -> "Not allowed"
            },
            trailing = {
                when {
                    shizuku == ShizukuState.PERMISSION_NEEDED -> TextButton(onClick = {
                        if (!viewModel.requestShizuku()) explaining = shizuku
                    }) { Text("Allow") }
                    shizukuApp != null -> TextButton(onClick = { context.startActivity(shizukuApp) }) {
                        Text(if (shizuku == ShizukuState.READY) "Manage" else "Open")
                    }
                }
            },
        )
        SettingRow(
            icon = R.drawable.ic_notifications,
            title = "Notifications",
            text = if (notifications) "Allowed" else "Not allowed",
            trailing = {
                TextButton(onClick = { context.startActivity(notificationSettings(context)) }) {
                    Text(if (notifications) "Manage" else "Allow")
                }
            },
        )

        SectionHeader("About")
        SettingRow(
            icon = R.drawable.ic_info,
            title = "Version",
            text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        )
        LinkRow(R.drawable.ic_code, "Source code", SOURCE_URL)
        LinkRow(R.drawable.ic_open_in_new, "Shizuku", SHIZUKU_URL)
    }

    explaining?.let { state ->
        ShizukuDialog(state, onHelp = null, onDismiss = { explaining = null })
    }

    if (choosingTheme) {
        ThemeDialog(
            selected = settings.theme,
            onSelect = { theme ->
                viewModel.updateSettings { it.copy(theme = theme) }
                choosingTheme = false
            },
            onDismiss = { choosingTheme = false },
        )
    }
}

@Composable
private fun LinkRow(icon: Int, title: String, url: String) {
    val context = LocalContext.current
    SettingRow(
        icon = icon,
        title = title,
        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) },
    )
}

@Composable
private fun ThemeDialog(selected: ThemeMode, onSelect: (ThemeMode) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Theme") },
        text = {
            Column(Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = theme == selected, role = Role.RadioButton) { onSelect(theme) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = theme == selected, onClick = null)
                        Text(
                            theme.label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun notificationSettings(context: Context) =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
