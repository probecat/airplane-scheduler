package dev.probecat.airplanescheduler.ui.schedules

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
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
import dev.probecat.airplanescheduler.ui.AppViewModel
import dev.probecat.airplanescheduler.ui.ContentWidth
import dev.probecat.airplanescheduler.ui.DismissibleSnackbarHost
import dev.probecat.airplanescheduler.ui.Format
import dev.probecat.airplanescheduler.ui.ShizukuDialog
import dev.probecat.airplanescheduler.ui.rememberFormat
import java.time.LocalDate
import kotlin.math.abs
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulesScreen(viewModel: AppViewModel, onSettings: () -> Unit, onHelp: () -> Unit, onDebug: () -> Unit) {
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    val shizuku by viewModel.shizuku.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
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
            snackbar.showSnackbar("This schedule overlaps ${format.label(conflict, now.toLocalDate())}.")
        }
    }

    fun delete(schedule: Schedule) {
        viewModel.delete(schedule)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar("Schedule deleted", "Undo", duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restore(schedule)
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Airplane Scheduler") },
                actions = { OverflowMenu(onSettings, onHelp, onDebug) },
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
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 112.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (shizuku != ShizukuState.READY || !settings.hideShizukuReady) {
                    item(key = "status") {
                        ShizukuStrip(
                            state = shizuku,
                            onClick = {
                                if (shizuku != ShizukuState.PERMISSION_NEEDED || !viewModel.requestShizuku()) {
                                    explaining = shizuku
                                }
                            },
                            onDismiss = { viewModel.updateSettings { it.copy(hideShizukuReady = true) } },
                            modifier = Modifier.animateItem().padding(bottom = 8.dp),
                        )
                    }
                }
                if (schedules.isEmpty()) {
                    item(key = "empty") { EmptyState(Modifier.fillParentMaxHeight(0.6f)) }
                }
                items(schedules, key = { it.id }) { schedule ->
                    SwipeToDelete(onDelete = { delete(schedule) }, modifier = Modifier.animateItem()) {
                        ScheduleCard(
                            schedule = schedule,
                            active = schedule.id == active?.id,
                            today = now.toLocalDate(),
                            format = format,
                            onClick = { editing = schedule },
                            onToggle = { enabled -> viewModel.setEnabled(schedule, enabled)?.let(::showConflict) },
                        )
                    }
                }
            }
        }
    }

    editing?.let { schedule ->
        ScheduleSheet(
            initial = schedule,
            edits = viewModel.draft(schedule),
            isNew = viewModel.isNew(schedule),
            now = now,
            conflictOf = viewModel::conflict,
            onDismiss = { editing = null },
            onEdit = { viewModel.keepDraft(schedule, it) },
            onSave = { viewModel.save(it) == null },
            onDelete = ::delete,
        )
    }

    explaining?.let { state ->
        ShizukuDialog(
            state,
            onHelp = {
                explaining = null
                onHelp()
            },
            onDismiss = { explaining = null },
        )
    }
}

@Composable
private fun OverflowMenu(onSettings: () -> Unit, onHelp: () -> Unit, onDebug: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(painterResource(R.drawable.ic_more_vert), contentDescription = "More options")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            listOf(
                Triple("Settings", R.drawable.ic_settings, onSettings),
                Triple("Help", R.drawable.ic_help, onHelp),
                Triple("Debug info", R.drawable.ic_bug_report, onDebug),
            ).forEach { (label, icon, action) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    leadingIcon = { Icon(painterResource(icon), contentDescription = null) },
                    onClick = {
                        open = false
                        action()
                    },
                )
            }
        }
    }
}

@Composable
private fun ShizukuStrip(
    state: ShizukuState,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val ready = state == ShizukuState.READY
    val (label, icon) = when (state) {
        ShizukuState.READY -> "Shizuku is ready" to R.drawable.ic_check_circle
        ShizukuState.OFFLINE -> "Shizuku is offline" to R.drawable.ic_error
        ShizukuState.PERMISSION_NEEDED -> "Allow Shizuku access" to R.drawable.ic_lock
    }
    Surface(
        onClick = onClick,
        enabled = !ready,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (ready) colors.surfaceContainer else colors.errorContainer,
        contentColor = if (ready) colors.onSurface else colors.onErrorContainer,
    ) {
        Row(
            modifier = Modifier.heightIn(min = 48.dp).padding(start = 16.dp, end = if (ready) 8.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = if (ready) colors.primary else LocalContentColor.current,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            if (ready) {
                TextButton(onClick = onDismiss, contentPadding = PaddingValues(horizontal = 12.dp)) { Text("Dismiss") }
            }
        }
    }
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
private fun SwipeToDelete(onDelete: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    // Not saveable: undo and app restarts can reuse an id, and a saved dismissed state would delete the
    // next schedule with it.
    val threshold = SwipeToDismissBoxDefaults.positionalThreshold
    val state = remember { SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, threshold) }
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        onDismiss = { onDelete() },
        backgroundContent = {
            val direction = state.dismissDirection
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Alignment.CenterStart
                } else {
                    Alignment.CenterEnd
                },
            ) {
                if (direction != SwipeToDismissBoxValue.Settled) {
                    val width = with(LocalDensity.current) { abs(state.requireOffset()).toDp() - 8.dp }
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxHeight().width(width.coerceAtLeast(0.dp)),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(painterResource(R.drawable.ic_delete), contentDescription = null)
                        }
                    }
                }
            }
        },
        content = { content() },
    )
}

@Composable
private fun ScheduleCard(
    schedule: Schedule,
    active: Boolean,
    today: LocalDate,
    format: Format,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(if (schedule.enabled) colors.primaryContainer else colors.surfaceContainer)
    val content = if (schedule.enabled) colors.onPrimaryContainer else colors.onSurfaceVariant
    // An em space between days and name.
    val summary = listOf(format.repeats(schedule, today), schedule.name)
        .filter { it.isNotBlank() }
        .joinToString("\u2003")
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
    ) {
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
                        contentDescription = "${format.window(schedule)}, ${format.repeats(schedule, today)}"
                    },
                )
            }
        }
    }
}

private val TIME_SIZE = 36.sp

@Composable
private fun ActiveBadge() {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(R.drawable.ic_airplanemode_active),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text("Active now", style = MaterialTheme.typography.labelMedium)
        }
    }
}
