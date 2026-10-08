package dev.probecat.airplanescheduler.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import dev.probecat.airplanescheduler.system.ShizukuState
import dev.probecat.airplanescheduler.system.ShizukuStatus

@Composable
fun ShizukuDialog(state: ShizukuState, onHelp: (() -> Unit)?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val shizuku = context.packageManager.getLaunchIntentForPackage(ShizukuStatus.SHIZUKU_PACKAGE)
    val (title, text) = when (state) {
        ShizukuState.READY -> return
        ShizukuState.OFFLINE ->
            "Shizuku isn't running" to
                "Airplane Scheduler switches airplane mode through Shizuku. Shizuku stops when the phone restarts, " +
                "so start it again in the Shizuku app. Until then, schedules can't switch anything."
        ShizukuState.PERMISSION_NEEDED ->
            "Allow access in Shizuku" to
                "Shizuku won't ask again. Open Shizuku, find Airplane Scheduler under authorized apps, and allow it."
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            if (shizuku != null) {
                TextButton(onClick = {
                    onDismiss()
                    context.startActivity(shizuku)
                }) { Text("Open Shizuku") }
            } else {
                TextButton(onClick = onDismiss) { Text("OK") }
            }
        },
        dismissButton = onHelp?.let { { TextButton(onClick = it) { Text("Help") } } },
    )
}
