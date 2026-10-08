package dev.probecat.airplanescheduler.ui.schedules

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.probecat.airplanescheduler.R
import dev.probecat.airplanescheduler.core.ScheduleResolver
import dev.probecat.airplanescheduler.data.Schedule
import dev.probecat.airplanescheduler.system.ShizukuState
import dev.probecat.airplanescheduler.system.ShizukuStatus
import dev.probecat.airplanescheduler.ui.AppViewModel
import dev.probecat.airplanescheduler.ui.ContentWidth
import dev.probecat.airplanescheduler.ui.DismissibleSnackbarHost
import dev.probecat.airplanescheduler.ui.Format
import dev.probecat.airplanescheduler.ui.rememberFormat
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulesScreen(viewModel: AppViewModel) {
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    val shizuku by viewModel.shizuku.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()
    val format = rememberFormat()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var editing by rememberSaveable(stateSaver = ScheduleSaver) { mutableStateOf<Schedule?>(null) }
    var explaining by rememberSaveable { mutableStateOf<ShizukuState?>(null) }
    val active = ScheduleResolver.activeSchedule(schedules, now)

    fun showConflict(conflict: Schedule) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar("Overlaps ${format.label(conflict)}. Switch that one off or change the times first.")
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Airplane Scheduler") },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editing = viewModel.newSchedule() },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.size(80.dp),
            ) {
                Icon(
                    painterResource(R.drawable.ic_add),
                    contentDescription = "Add schedule",
                    modifier = Modifier.size(28.dp),
                )
            }
        },
        snackbarHost = { DismissibleSnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = ContentWidth).fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 112.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "status") {
                    ShizukuChip(shizuku) {
                        if (shizuku != ShizukuState.PERMISSION_NEEDED || !viewModel.requestShizuku()) {
                            explaining = shizuku
                        }
                    }
                }
                if (schedules.isEmpty()) {
                    item(key = "empty") { EmptyState(Modifier.fillParentMaxHeight(0.6f)) }
                }
                items(schedules, key = { it.id }) { schedule ->
                    ScheduleCard(
                        schedule = schedule,
                        active = schedule.id == active?.id,
                        format = format,
                        onClick = { editing = schedule },
                        onToggle = { enabled -> viewModel.setEnabled(schedule, enabled)?.let(::showConflict) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    editing?.let { schedule ->
        ScheduleSheet(
            initial = schedule,
            isNew = viewModel.isNew(schedule),
            conflictOf = viewModel::conflict,
            onDismiss = { editing = null },
            onSave = { viewModel.save(it) == null },
            onDelete = { deleted ->
                viewModel.delete(deleted)
                scope.launch {
                    snackbar.currentSnackbarData?.dismiss()
                    val result = snackbar.showSnackbar("Schedule deleted", "Undo", duration = SnackbarDuration.Long)
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.restore(deleted)
                    }
                }
            },
        )
    }

    explaining?.let { state ->
        ShizukuDialog(state, onDismiss = { explaining = null })
    }
}

// A tappable chip when Shizuku needs attention; when it's ready, the same look as a plain badge.
@Composable
private fun ShizukuChip(state: ShizukuState, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val (label, icon) = when (state) {
        ShizukuState.READY -> "Shizuku ready" to R.drawable.ic_check_circle
        ShizukuState.OFFLINE -> "Shizuku offline" to R.drawable.ic_error
        ShizukuState.PERMISSION_NEEDED -> "Allow Shizuku access" to R.drawable.ic_lock
    }
    val leadingIcon = @Composable {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = if (state == ShizukuState.READY) colors.primary else colors.error,
            modifier = Modifier.size(AssistChipDefaults.IconSize),
        )
    }
    if (state == ShizukuState.READY) {
        // The chip reserves a 48 dp touch target around its 32 dp body, so the badge does too, and
        // nothing below moves when the state changes.
        Surface(
            modifier = Modifier.minimumInteractiveComponentSize(),
            shape = AssistChipDefaults.shape,
            border = AssistChipDefaults.assistChipBorder(enabled = true),
            color = Color.Transparent,
        ) {
            Row(
                modifier = Modifier.height(AssistChipDefaults.Height).padding(start = 8.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leadingIcon()
                Spacer(Modifier.width(8.dp))
                Text(label, style = MaterialTheme.typography.labelLarge, color = colors.onSurface)
            }
        }
    } else {
        AssistChip(onClick = onClick, label = { Text(label) }, leadingIcon = leadingIcon)
    }
}

@Composable
private fun ShizukuDialog(state: ShizukuState, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val shizuku = context.packageManager.getLaunchIntentForPackage(ShizukuStatus.SHIZUKU_PACKAGE)
    val (title, text) = when (state) {
        ShizukuState.READY -> return
        ShizukuState.OFFLINE -> "Shizuku isn't running" to
            "Airplane Scheduler switches airplane mode through Shizuku. Shizuku stops when the phone restarts, " +
            "so start it again in the Shizuku app. Until then, schedules can't switch anything."
        ShizukuState.PERMISSION_NEEDED -> "Allow access in Shizuku" to
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
    )
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
            Icon(
                painterResource(R.drawable.ic_airplanemode_active),
                contentDescription = null,
                modifier = Modifier.padding(20.dp).size(40.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("No schedules yet", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun ScheduleCard(
    schedule: Schedule,
    active: Boolean,
    format: Format,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(if (schedule.enabled) colors.primaryContainer else colors.surfaceContainer)
    val content = if (schedule.enabled) colors.onPrimaryContainer else colors.onSurfaceVariant
    val summary = listOf(format.days(schedule.days), schedule.name).filter { it.isNotBlank() }.joinToString("  ·  ")
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
    ) {
        // Sized like Clock's alarm cards: a summary line over a tall row with the times and switch.
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    summary,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (active) {
                    Spacer(Modifier.width(8.dp))
                    ActiveBadge()
                }
            }
            Row(Modifier.heightIn(min = 59.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    format.window(schedule, Format.SMALL),
                    style = MaterialTheme.typography.displayMedium,
                    color = if (schedule.enabled) colors.onPrimaryContainer else colors.onSurface,
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 20.sp, maxFontSize = TIME_SIZE),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = schedule.enabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.semantics {
                        contentDescription = "${format.window(schedule)}, ${format.days(schedule.days)}"
                    },
                )
            }
        }
    }
}

private val TIME_SIZE = 40.sp

@Composable
private fun ActiveBadge() {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_airplanemode_active), contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("Active now", style = MaterialTheme.typography.labelMedium)
        }
    }
}
