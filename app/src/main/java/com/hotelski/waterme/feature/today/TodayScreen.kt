package com.hotelski.waterme.feature.today

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hotelski.waterme.feature.common.CareTaskCard
import com.hotelski.waterme.feature.common.CareTaskUiModel
import com.hotelski.waterme.feature.common.HealthNoteRow
import com.hotelski.waterme.feature.common.HealthNoteUiModel
import com.hotelski.waterme.feature.common.PlantCard
import com.hotelski.waterme.feature.common.PlantCardUiModel
import com.hotelski.waterme.feature.common.ReminderRow
import com.hotelski.waterme.feature.common.ReminderUiModel
import com.hotelski.waterme.feature.common.WaterMeCard
import com.hotelski.waterme.feature.common.WaterMeEmptyState
import com.hotelski.waterme.feature.common.WaterMeErrorState
import com.hotelski.waterme.feature.common.WaterMeLoadingState
import com.hotelski.waterme.feature.common.WaterMePreviewData
import com.hotelski.waterme.feature.common.WaterMeSectionHeader
import com.hotelski.waterme.feature.common.WaterMeTopBar
import com.hotelski.waterme.feature.characters.PlantCharacterCelebrationCard
import com.hotelski.waterme.feature.characters.PlantCharacterUiModel
import com.hotelski.waterme.ui.theme.Clay
import com.hotelski.waterme.ui.theme.FreshGreen
import com.hotelski.waterme.ui.theme.GardenBackground
import com.hotelski.waterme.ui.theme.Ink
import com.hotelski.waterme.ui.theme.LeafGreen
import com.hotelski.waterme.ui.theme.MistBlue
import com.hotelski.waterme.ui.theme.MutedInk
import com.hotelski.waterme.ui.theme.SoftCream
import com.hotelski.waterme.ui.theme.WaterMeTheme

data class DashboardProgressUiModel(
    val completedToday: Int = 0,
    val dueToday: Int = 0,
    val overdue: Int = 0,
    val completionPercent: Float = 0f,
)

data class PlantHealthSummaryUiModel(
    val attentionCount: Int = 0,
    val healthyCount: Int = 0,
    val newGrowthCount: Int = 0,
) {
    val totalNotes: Int
        get() = attentionCount + healthyCount + newGrowthCount
}

data class TodayUiState(
    val isLoading: Boolean = false,
    val tasks: List<CareTaskUiModel> = emptyList(),
    val overdueTasks: List<CareTaskUiModel> = emptyList(),
    val upcomingReminders: List<ReminderUiModel> = emptyList(),
    val healthNotes: List<HealthNoteUiModel> = emptyList(),
    val healthSummary: PlantHealthSummaryUiModel = PlantHealthSummaryUiModel(),
    val recentlyAddedPlants: List<PlantCardUiModel> = emptyList(),
    val progressStats: DashboardProgressUiModel = DashboardProgressUiModel(),
    val plantCount: Int = 0,
    val reminderCount: Int = 0,
    val activeCharacter: PlantCharacterUiModel? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
) {
    val hasNoPlants: Boolean
        get() = !isLoading && plantCount == 0

    val totalOpenTasks: Int
        get() = tasks.size + overdueTasks.size

    val shouldShowCharacterCelebration: Boolean
        get() = activeCharacter != null && successMessage?.contains("completed", ignoreCase = true) == true
}

sealed interface TodayEvent {
    data object AddPlantClicked : TodayEvent
    data object CalendarClicked : TodayEvent
    data object MyPlantsClicked : TodayEvent
    data object RetryClicked : TodayEvent
    data class PlantClicked(val plantId: String) : TodayEvent
    data class CompleteTask(val taskId: String) : TodayEvent
    data class SkipTask(val taskId: String) : TodayEvent
    data class SnoozeTask(val taskId: String) : TodayEvent
}

@Composable
fun TodayScreen(
    uiState: TodayUiState,
    onEvent: (TodayEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = GardenBackground,
        topBar = { WaterMeTopBar(title = "Home") },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEvent(TodayEvent.AddPlantClicked) },
                containerColor = LeafGreen,
                contentColor = Color.White,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add plant")
            }
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> WaterMeLoadingState("Building your plant dashboard...", Modifier.padding(innerPadding))
            uiState.errorMessage != null -> Box(Modifier.padding(innerPadding).padding(20.dp)) {
                WaterMeErrorState(
                    message = uiState.errorMessage,
                    onRetryClick = { onEvent(TodayEvent.RetryClicked) },
                )
            }

            else -> HomeDashboard(
                uiState = uiState,
                onEvent = onEvent,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun HomeDashboard(
    uiState: TodayUiState,
    onEvent: (TodayEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(GardenBackground),
    ) {
        if (maxWidth >= 840.dp) {
            TabletDashboard(uiState = uiState, onEvent = onEvent)
        } else {
            PhoneDashboard(uiState = uiState, onEvent = onEvent)
        }
    }
}

@Composable
private fun PhoneDashboard(
    uiState: TodayUiState,
    onEvent: (TodayEvent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { DashboardHeroCard(uiState) }
        if (uiState.shouldShowCharacterCelebration) {
            item {
                PlantCharacterCelebrationCard(
                    character = requireNotNull(uiState.activeCharacter),
                    message = uiState.successMessage.orEmpty(),
                )
            }
        }
        item { QuickActionsCard(onEvent) }

        if (uiState.hasNoPlants) {
            item {
                WaterMeEmptyState(
                    title = "Start your plant list",
                    message = "Add a plant to see care tasks, reminders, notes, and progress here.",
                    actionLabel = "Add plant",
                    onActionClick = { onEvent(TodayEvent.AddPlantClicked) },
                )
            }
        }

        dashboardTaskSection(
            title = "Overdue",
            tasks = uiState.overdueTasks,
            emptyTitle = "No overdue care",
            emptyMessage = "Everything urgent is handled.",
            onEvent = onEvent,
            showWhenEmpty = uiState.plantCount > 0,
        )

        dashboardTaskSection(
            title = "Today",
            tasks = uiState.tasks,
            emptyTitle = "No care due today",
            emptyMessage = "Your plants are on schedule. Upcoming care is waiting below.",
            onEvent = onEvent,
            showWhenEmpty = uiState.plantCount > 0,
        )

        item { ProgressStatsCard(uiState.progressStats) }
        item { UpcomingRemindersSection(uiState.upcomingReminders) }
        item { PlantHealthSummaryCard(uiState.healthSummary, uiState.healthNotes) }
        item { RecentlyAddedPlantsSection(uiState.recentlyAddedPlants, onEvent) }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun TabletDashboard(
    uiState: TodayUiState,
    onEvent: (TodayEvent) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { DashboardHeroCard(uiState) }
            if (uiState.shouldShowCharacterCelebration) {
                item {
                    PlantCharacterCelebrationCard(
                        character = requireNotNull(uiState.activeCharacter),
                        message = uiState.successMessage.orEmpty(),
                    )
                }
            }
            item { QuickActionsCard(onEvent) }
            dashboardTaskSection(
                title = "Overdue",
                tasks = uiState.overdueTasks,
                emptyTitle = "No overdue care",
                emptyMessage = "Everything urgent is handled.",
                onEvent = onEvent,
                showWhenEmpty = uiState.plantCount > 0,
            )
            dashboardTaskSection(
                title = "Today",
                tasks = uiState.tasks,
                emptyTitle = "No care due today",
                emptyMessage = "Your plants are on schedule.",
                onEvent = onEvent,
                showWhenEmpty = uiState.plantCount > 0,
            )
            item { Spacer(Modifier.height(80.dp)) }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { ProgressStatsCard(uiState.progressStats) }
            item { UpcomingRemindersSection(uiState.upcomingReminders) }
            item { PlantHealthSummaryCard(uiState.healthSummary, uiState.healthNotes) }
            item { RecentlyAddedPlantsSection(uiState.recentlyAddedPlants, onEvent) }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.dashboardTaskSection(
    title: String,
    tasks: List<CareTaskUiModel>,
    emptyTitle: String,
    emptyMessage: String,
    onEvent: (TodayEvent) -> Unit,
    showWhenEmpty: Boolean,
) {
    item { WaterMeSectionHeader(title) }
    if (tasks.isEmpty()) {
        if (showWhenEmpty) {
            item {
                WaterMeEmptyState(
                    title = emptyTitle,
                    message = emptyMessage,
                    icon = if (title == "Overdue") Icons.Rounded.Check else Icons.Rounded.Event,
                )
            }
        }
    } else {
        items(tasks, key = { "${title}-${it.id}" }) { task ->
            SwipeCareTaskCard(task = task, onEvent = onEvent)
        }
    }
}

@Composable
private fun DashboardHeroCard(uiState: TodayUiState) {
    WaterMeCard(containerColor = LeafGreen) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    Icons.Rounded.LocalFlorist,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            uiState.hasNoPlants -> "Welcome to WaterMe"
                            uiState.overdueTasks.isNotEmpty() -> "${uiState.overdueTasks.size} overdue task${if (uiState.overdueTasks.size == 1) "" else "s"}"
                            uiState.tasks.isEmpty() -> "All clear today"
                            else -> "${uiState.tasks.size} task${if (uiState.tasks.size == 1) "" else "s"} due today"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = when {
                            uiState.hasNoPlants -> "Add your first plant and WaterMe will build the care plan."
                            uiState.totalOpenTasks == 0 -> "Your plants are on schedule. Check upcoming reminders for what is next."
                            else -> "Log care as you go and WaterMe will keep the next reminder moving."
                        },
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.88f),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DashboardChip("${uiState.plantCount} plants", Modifier.weight(1f))
                DashboardChip("${uiState.reminderCount} reminders", Modifier.weight(1f))
                DashboardChip("${uiState.progressStats.completedToday} done", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DashboardChip(
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun QuickActionsCard(onEvent: (TodayEvent) -> Unit) {
    WaterMeCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            WaterMeSectionHeader("Quick actions")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickActionButton(
                    label = "Add",
                    icon = Icons.Rounded.Add,
                    onClick = { onEvent(TodayEvent.AddPlantClicked) },
                    modifier = Modifier.weight(1f),
                )
                QuickActionButton(
                    label = "Calendar",
                    icon = Icons.Rounded.CalendarMonth,
                    onClick = { onEvent(TodayEvent.CalendarClicked) },
                    modifier = Modifier.weight(1f),
                )
                QuickActionButton(
                    label = "Plants",
                    icon = Icons.Rounded.Spa,
                    onClick = { onEvent(TodayEvent.MyPlantsClicked) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeCareTaskCard(
    task: CareTaskUiModel,
    onEvent: (TodayEvent) -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onEvent(TodayEvent.CompleteTask(task.id))
                    true
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onEvent(TodayEvent.SnoozeTask(task.id))
                    true
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val targetValue = dismissState.targetValue
            val backgroundColor by animateColorAsState(
                targetValue = when (targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> FreshGreen
                    SwipeToDismissBoxValue.EndToStart -> Clay
                    SwipeToDismissBoxValue.Settled -> GardenBackground
                },
                label = "SwipeTaskBackground",
            )
            val icon = when (targetValue) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Rounded.Check
                SwipeToDismissBoxValue.EndToStart -> Icons.Rounded.Snooze
                SwipeToDismissBoxValue.Settled -> Icons.Rounded.Eco
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor, RoundedCornerShape(24.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = when (targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    else -> Alignment.CenterStart
                },
            ) {
                Icon(icon, contentDescription = null, tint = Color.White)
            }
        },
    ) {
        CareTaskCard(
            task = task,
            onOpenPlant = { onEvent(TodayEvent.PlantClicked(task.plantId)) },
            onComplete = { onEvent(TodayEvent.CompleteTask(task.id)) },
            onSkip = { onEvent(TodayEvent.SkipTask(task.id)) },
            onSnooze = { onEvent(TodayEvent.SnoozeTask(task.id)) },
        )
    }
}

@Composable
private fun ProgressStatsCard(progressStats: DashboardProgressUiModel) {
    val progress by animateFloatAsState(
        targetValue = progressStats.completionPercent.coerceIn(0f, 1f),
        label = "DashboardProgress",
    )

    WaterMeCard {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            WaterMeSectionHeader("Progress")
            Row(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(86.dp),
                        color = LeafGreen,
                        trackColor = LeafGreen.copy(alpha = 0.14f),
                        strokeWidth = 8.dp,
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ProgressStatRow("Completed today", progressStats.completedToday, LeafGreen)
                    ProgressStatRow("Still due", progressStats.dueToday, MistBlue)
                    ProgressStatRow("Overdue", progressStats.overdue, Clay)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = LeafGreen,
                        trackColor = LeafGreen.copy(alpha = 0.12f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressStatRow(
    label: String,
    value: Int,
    color: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, RoundedCornerShape(4.dp)),
            )
            Spacer(Modifier.width(8.dp))
            Text(label, color = MutedInk)
        }
        Text(value.toString(), color = Ink, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun UpcomingRemindersSection(reminders: List<ReminderUiModel>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        WaterMeSectionHeader("Upcoming reminders")
        if (reminders.isEmpty()) {
            WaterMeEmptyState(
                title = "No upcoming reminders",
                message = "Add or edit a plant schedule to see what is coming next.",
                icon = Icons.Rounded.CalendarMonth,
            )
        } else {
            reminders.forEach { reminder -> ReminderRow(reminder = reminder) }
        }
    }
}

@Composable
private fun PlantHealthSummaryCard(
    summary: PlantHealthSummaryUiModel,
    healthNotes: List<HealthNoteUiModel>,
) {
    WaterMeCard(containerColor = SoftCream) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            WaterMeSectionHeader("Plant health")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HealthMetric("Attention", summary.attentionCount, Clay, Modifier.weight(1f))
                HealthMetric("Healthy", summary.healthyCount, LeafGreen, Modifier.weight(1f))
                HealthMetric("Growth", summary.newGrowthCount, FreshGreen, Modifier.weight(1f))
            }
            if (healthNotes.isEmpty()) {
                Text(
                    text = "No recent health notes. Log observations from a plant detail page.",
                    color = MutedInk,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    healthNotes.take(2).forEach { note -> HealthNoteRow(note) }
                }
            }
        }
    }
}

@Composable
private fun HealthMetric(
    label: String,
    value: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(value.toString(), color = color, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, color = MutedInk, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun RecentlyAddedPlantsSection(
    plants: List<PlantCardUiModel>,
    onEvent: (TodayEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        WaterMeSectionHeader("Recently added")
        if (plants.isEmpty()) {
            WaterMeEmptyState(
                title = "No plants yet",
                message = "Your newest plants will appear here after you add them.",
                actionLabel = "Add plant",
                onActionClick = { onEvent(TodayEvent.AddPlantClicked) },
            )
        } else {
            plants.forEach { plant ->
                PlantCard(
                    plant = plant,
                    onClick = { onEvent(TodayEvent.PlantClicked(plant.id)) },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeDashboardPreview() {
    WaterMeTheme {
        TodayScreen(
            uiState = dashboardPreviewState(),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 920, heightDp = 760)
@Composable
private fun HomeDashboardTabletPreview() {
    WaterMeTheme {
        TodayScreen(
            uiState = dashboardPreviewState(),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeDashboardEmptyPreview() {
    WaterMeTheme {
        TodayScreen(
            uiState = TodayUiState(),
            onEvent = {},
        )
    }
}

private fun dashboardPreviewState(): TodayUiState =
    TodayUiState(
        tasks = WaterMePreviewData.tasks.take(2),
        overdueTasks = WaterMePreviewData.tasks.takeLast(1),
        upcomingReminders = WaterMePreviewData.reminders,
        healthNotes = WaterMePreviewData.healthNotes,
        healthSummary = PlantHealthSummaryUiModel(attentionCount = 1, healthyCount = 1, newGrowthCount = 1),
        recentlyAddedPlants = WaterMePreviewData.plants,
        progressStats = DashboardProgressUiModel(
            completedToday = 4,
            dueToday = 3,
            overdue = 1,
            completionPercent = 0.57f,
        ),
        plantCount = WaterMePreviewData.plants.size,
        reminderCount = WaterMePreviewData.reminders.size,
    )
