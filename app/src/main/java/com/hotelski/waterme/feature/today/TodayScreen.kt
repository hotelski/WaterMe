package com.hotelski.waterme.feature.today

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hotelski.waterme.feature.characters.PlantCharacterCelebrationCard
import com.hotelski.waterme.feature.characters.PlantCharacterAvatar
import com.hotelski.waterme.feature.characters.PlantCharacterUiModel
import com.hotelski.waterme.feature.common.CareTaskUiModel
import com.hotelski.waterme.feature.common.HealthNoteUiModel
import com.hotelski.waterme.feature.common.PlantCardUiModel
import com.hotelski.waterme.feature.common.PlantPhotoTile
import com.hotelski.waterme.feature.common.ReminderUiModel
import com.hotelski.waterme.feature.common.WaterMeCard
import com.hotelski.waterme.feature.common.WaterMeEmptyState
import com.hotelski.waterme.feature.common.WaterMeErrorState
import com.hotelski.waterme.feature.common.WaterMeLeafRefreshBox
import com.hotelski.waterme.feature.common.WaterMeLoadingState
import com.hotelski.waterme.feature.common.WaterMePreviewData
import com.hotelski.waterme.feature.common.WaterMeTopBar
import com.hotelski.waterme.feature.common.accentColor
import com.hotelski.waterme.feature.common.icon
import com.hotelski.waterme.feature.common.label
import com.hotelski.waterme.ui.theme.Clay
import com.hotelski.waterme.ui.theme.FreshGreen
import com.hotelski.waterme.ui.theme.LeafGreen
import com.hotelski.waterme.ui.theme.MistBlue
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
    val careHistoryCount: Int = 0,
    val noteCount: Int = 0,
    val plantScannerScanCount: Int = 0,
    val aiCareUsageCount: Int = 0,
    val appOpenDayStreak: Int = 0,
    val completedThisWeek: Int = 0,
    val activeCharacter: PlantCharacterUiModel? = null,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val heartBurstKey: Long = 0L,
) {
    val hasNoPlants: Boolean
        get() = !isLoading && plantCount == 0

    val totalOpenTasks: Int
        get() = tasks.size + overdueTasks.size

    val shouldShowCharacterCelebration: Boolean
        get() = activeCharacter != null && successMessage != null
}

sealed interface TodayEvent {
    data object AddPlantClicked : TodayEvent
    data object CalendarClicked : TodayEvent
    data object DonateClicked : TodayEvent
    data object FeedbackClicked : TodayEvent
    data object HowToUseClicked : TodayEvent
    data object MyPlantsClicked : TodayEvent
    data object RetryClicked : TodayEvent
    data object RefreshPulled : TodayEvent
    data class CompleteTask(val taskId: String) : TodayEvent
    data class SkipTask(val taskId: String) : TodayEvent
    data class SnoozeTask(val taskId: String) : TodayEvent
}

@Composable
fun TodayScreen(
    uiState: TodayUiState,
    onEvent: (TodayEvent) -> Unit,
    modifier: Modifier = Modifier,
    onHowToUseClick: () -> Unit = {},
    onFeedbackClick: () -> Unit = {},
    onDonateClick: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            WaterMeTopBar(
                title = "Home",
                tertiaryActionIcon = Icons.Rounded.Info,
                tertiaryActionContentDescription = "How to use WaterMe",
                onTertiaryActionClick = onHowToUseClick,
                secondaryActionIcon = Icons.Rounded.VolunteerActivism,
                secondaryActionContentDescription = "Support creator",
                onSecondaryActionClick = onDonateClick,
                actionIcon = Icons.Rounded.RateReview,
                actionContentDescription = "Share feedback",
                onActionClick = onFeedbackClick,
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> WaterMeLoadingState(
                message = "Preparing your garden...",
                modifier = Modifier.padding(innerPadding),
            )

            uiState.errorMessage != null -> Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(20.dp),
            ) {
                WaterMeErrorState(
                    message = uiState.errorMessage,
                    onRetryClick = { onEvent(TodayEvent.RetryClicked) },
                )
            }

            else -> WaterMeLeafRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { onEvent(TodayEvent.RefreshPulled) },
                modifier = Modifier.padding(innerPadding),
            ) {
                HomeDashboard(
                    uiState = uiState,
                    onEvent = onEvent,
                )
            }
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
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (maxWidth >= 840.dp) {
            TabletDashboard(
                uiState = uiState,
                onEvent = onEvent,
            )
        } else {
            PhoneDashboard(
                uiState = uiState,
                onEvent = onEvent,
            )
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
        item { CharacterGreetingCard(uiState) }

        item { GardenStatsCard(uiState) }

        if (uiState.shouldShowCharacterCelebration) {
            item {
                PlantCharacterCelebrationCard(
                    character = requireNotNull(uiState.activeCharacter),
                    message = uiState.successMessage.orEmpty(),
                    heartBurstKey = uiState.heartBurstKey.takeIf { it != 0L },
                )
            }
        }

        if (uiState.hasNoPlants) {
            item {
                WaterMeEmptyState(
                    title = "Your garden is empty",
                    message = "Add plants from the Plants page and WaterMe will start building your daily care plan.",
                )
            }
        }

        item {
            CareTasksSection(
                overdueTasks = uiState.overdueTasks,
                todayTasks = uiState.tasks,
                onEvent = onEvent,
                showWhenEmpty = uiState.plantCount > 0,
            )
        }

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
            item { CharacterGreetingCard(uiState) }
            item { GardenStatsCard(uiState) }

            if (uiState.shouldShowCharacterCelebration) {
                item {
                    PlantCharacterCelebrationCard(
                        character = requireNotNull(uiState.activeCharacter),
                        message = uiState.successMessage.orEmpty(),
                        heartBurstKey = uiState.heartBurstKey.takeIf { it != 0L },
                    )
                }
            }

            if (uiState.hasNoPlants) {
                item {
                    WaterMeEmptyState(
                        title = "Your garden is empty",
                        message = "Add plants from the Plants page and WaterMe will start building your daily care plan.",
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                CareTasksSection(
                    overdueTasks = uiState.overdueTasks,
                    todayTasks = uiState.tasks,
                    onEvent = onEvent,
                    showWhenEmpty = uiState.plantCount > 0,
                )
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun DashboardHeroCard(uiState: TodayUiState) {
    val openTasks = uiState.totalOpenTasks

    val title = when {
        uiState.hasNoPlants -> "Welcome to WaterMe"
        uiState.overdueTasks.isNotEmpty() -> "${uiState.overdueTasks.size} overdue task${if (uiState.overdueTasks.size == 1) "" else "s"}"
        uiState.tasks.isEmpty() -> "All clear today"
        else -> "${uiState.tasks.size} task${if (uiState.tasks.size == 1) "" else "s"} due today"
    }

    val message = when {
        uiState.hasNoPlants -> "Your smart plant care dashboard will appear here once you add plants."
        openTasks == 0 -> "A quiet day for your garden. Everything looks beautifully on track."
        uiState.overdueTasks.isNotEmpty() -> "Start with overdue care first to bring your garden back into balance."
        else -> "Complete today’s care and keep your plants thriving."
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        LeafGreen,
                        FreshGreen,
                    ),
                ),
                shape = RoundedCornerShape(32.dp),
            )
            .padding(22.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(92.dp)
                .background(
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(999.dp),
                ),
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(top = 96.dp)
                .size(44.dp)
                .background(
                    color = Color.White.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(999.dp),
                ),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(18.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocalFlorist,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )

                    Text(
                        text = message,
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.90f),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DashboardChip(
                    label = "${uiState.plantCount} plants",
                    modifier = Modifier.weight(1f),
                )

                DashboardChip(
                    label = "${uiState.progressStats.completedToday} done",
                    modifier = Modifier.weight(1f),
                )

                DashboardChip(
                    label = "${uiState.totalOpenTasks} open",
                    modifier = Modifier.weight(1f),
                )
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
            .background(
                color = Color.White.copy(alpha = 0.18f),
                shape = RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 10.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CharacterGreetingCard(uiState: TodayUiState) {
    val character = uiState.activeCharacter ?: return
    val accent = Color(character.accentColor)
    val shape = RoundedCornerShape(30.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
                    ),
                ),
                shape = shape,
            )
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlantCharacterAvatar(
                character = character,
                size = 78.dp,
                animated = true,
                showBackdrop = true,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Hi, I'm ${character.name}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                }

                CharacterSpeechBubble(
                    message = uiState.characterGreetingMessage(),
                    accent = accent,
                )
            }
        }
    }
}

@Composable
private fun CharacterSpeechBubble(
    message: String,
    accent: Color,
) {
    Box {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 2.dp)
                .size(12.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(3.dp)),
        )

        Column(
            modifier = Modifier
                .padding(start = 6.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .padding(horizontal = 13.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 10.dp)
                .size(7.dp)
                .background(accent.copy(alpha = 0.30f), RoundedCornerShape(999.dp)),
        )
    }
}

private fun TodayUiState.characterGreetingMessage(): String =
    when {
        hasNoPlants -> "Add your first plant and I'll help watch the care rhythm."
        overdueTasks.isNotEmpty() -> "Let's start with overdue care. Your plants will feel better after this."
        tasks.isNotEmpty() -> "I found ${tasks.size} care task${if (tasks.size == 1) "" else "s"} for today. Small steps keep the garden thriving."
        else -> "Everything is calm today. I’ll keep an eye on the next reminder."
    }

@Composable
private fun GardenStatsCard(uiState: TodayUiState) {
    WaterMeCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Garden stats",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Text(
                    text = "Care rhythm and local garden data in one place.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStatCard(
                    label = "Day streak",
                    value = uiState.appOpenDayStreak.toString(),
                    helper = "active days",
                    color = LeafGreen,
                    icon = Icons.Rounded.Whatshot,
                    modifier = Modifier.weight(1f),
                )

                MiniStatCard(
                    label = "This week",
                    value = uiState.completedThisWeek.toString(),
                    helper = "completed",
                    color = FreshGreen,
                    icon = Icons.Rounded.Check,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStatCard(
                    label = "Plants",
                    value = uiState.plantCount.toString(),
                    helper = "tracked",
                    color = LeafGreen,
                    icon = Icons.Rounded.LocalFlorist,
                    modifier = Modifier.weight(1f),
                )

                MiniStatCard(
                    label = "Reminders",
                    value = uiState.reminderCount.toString(),
                    helper = "active",
                    color = MistBlue,
                    icon = Icons.Rounded.Event,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStatCard(
                    label = "Care logs",
                    value = uiState.careHistoryCount.toString(),
                    helper = "history",
                    color = Clay,
                    icon = Icons.Rounded.Check,
                    modifier = Modifier.weight(1f),
                )

                MiniStatCard(
                    label = "Notes",
                    value = uiState.noteCount.toString(),
                    helper = "care notes",
                    color = Color(0xFF6AA9A5),
                    icon = Icons.Rounded.RateReview,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStatCard(
                    label = "Scanner",
                    value = uiState.plantScannerScanCount.toString(),
                    helper = "plants scanned",
                    color = Color(0xFF4F8FC7),
                    icon = Icons.Rounded.PhotoCamera,
                    modifier = Modifier.weight(1f),
                )

                MiniStatCard(
                    label = "AI Care",
                    value = uiState.aiCareUsageCount.toString(),
                    helper = "requests used",
                    color = Color(0xFF8B6CB7),
                    icon = Icons.Rounded.AutoAwesome,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MiniStatCard(
    label: String,
    value: String,
    helper: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Column(
        modifier = modifier
            .background(
                color = color.copy(alpha = 0.12f),
                shape = RoundedCornerShape(22.dp),
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(color.copy(alpha = 0.16f), RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = helper,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CareTaskOverviewBar(
    overdueCount: Int,
    todayCount: Int,
) {
    WaterMeCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CareTaskOverviewMetric(
                title = if (overdueCount == 0) "All clear" else "Attention",
                detail = "$overdueCount overdue",
                color = if (overdueCount == 0) LeafGreen else Clay,
                icon = Icons.Rounded.Check,
                modifier = Modifier.weight(1f),
            )

            CareTaskOverviewMetric(
                title = "Today",
                detail = "$todayCount due",
                color = LeafGreen,
                icon = Icons.Rounded.Event,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CareTaskOverviewMetric(
    title: String,
    detail: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(20.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(color.copy(alpha = 0.16f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = detail,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CareTasksSection(
    overdueTasks: List<CareTaskUiModel>,
    todayTasks: List<CareTaskUiModel>,
    onEvent: (TodayEvent) -> Unit,
    showWhenEmpty: Boolean,
) {
    val overdueCount = overdueTasks.size
    val todayCount = todayTasks.size

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CareTasksHeader()

        CareTaskOverviewBar(
            overdueCount = overdueCount,
            todayCount = todayCount,
        )

        if (overdueTasks.isEmpty() && todayTasks.isEmpty()) {
            if (showWhenEmpty) {
                NoCareTasksCard()
            }
        } else {
            if (overdueTasks.isNotEmpty()) {
                TaskGroupHeader(
                    title = "Overdue",
                    accentColor = Clay,
                )

                overdueTasks.forEach { task ->
                    SwipeCareTaskCard(
                        task = task,
                        onEvent = onEvent,
                    )
                }
            }

            if (todayTasks.isNotEmpty()) {
                TaskGroupHeader(
                    title = "Today",
                    accentColor = LeafGreen,
                )

                todayTasks.forEach { task ->
                    SwipeCareTaskCard(
                        task = task,
                        onEvent = onEvent,
                    )
                }
            }
        }
    }
}

@Composable
private fun NoCareTasksCard() {
    WaterMeCard(
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.46f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(LeafGreen.copy(alpha = 0.14f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = LeafGreen,
                    modifier = Modifier.size(24.dp),
                )
            }

            Text(
                text = "No care tasks",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Text(
                text = "Your plants are calm and on schedule.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CareTasksHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = "Care tasks",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Due and overdue plant care.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TaskGroupHeader(
    title: String,
    accentColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(accentColor, RoundedCornerShape(999.dp)),
        )

        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun HomeCareTaskCard(
    task: CareTaskUiModel,
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    onSnooze: () -> Unit,
) {
    val accentColor = task.careType.accentColor()

    WaterMeCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    PlantPhotoTile(
                        photoUri = task.plantPhotoUri,
                        plantName = task.plantName,
                        size = 54.dp,
                    )
                    Icon(
                        imageVector = task.careType.icon(),
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(22.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(9.dp))
                            .padding(4.dp),
                        tint = accentColor,
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = task.careType.label(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Text(
                        text = task.plantName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    CareTaskStatusChip(task)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onComplete,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(15.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LeafGreen),
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Done",
                        modifier = Modifier.padding(start = 6.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                OutlinedButton(
                    onClick = onSnooze,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(15.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                ) {
                    Icon(Icons.Rounded.Snooze, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Snooze",
                        modifier = Modifier.padding(start = 6.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                TextButton(
                    onClick = onSkip,
                    modifier = Modifier
                        .weight(0.72f)
                        .height(42.dp),
                    shape = RoundedCornerShape(15.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = LeafGreen),
                ) {
                    Text(
                        text = "Skip",
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun CareTaskStatusChip(task: CareTaskUiModel) {
    val color = when {
        task.isOverdue -> Clay
        task.isSnoozed -> MistBlue
        else -> LeafGreen
    }
    val label = when {
        task.isOverdue -> "Overdue"
        task.isSnoozed -> "Snoozed"
        else -> task.dueLabel
    }

    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
                    SwipeToDismissBoxValue.Settled -> MaterialTheme.colorScheme.background
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
                    .background(
                        color = backgroundColor,
                        shape = RoundedCornerShape(24.dp),
                    )
                    .padding(horizontal = 20.dp),
                contentAlignment = when (targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    else -> Alignment.CenterStart
                },
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                )
            }
        },
    ) {
        HomeCareTaskCard(
            task = task,
            onComplete = { onEvent(TodayEvent.CompleteTask(task.id)) },
            onSkip = { onEvent(TodayEvent.SkipTask(task.id)) },
            onSnooze = { onEvent(TodayEvent.SnoozeTask(task.id)) },
        )
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
        healthNotes = WaterMePreviewData.healthNotes,
        healthSummary = PlantHealthSummaryUiModel(
            attentionCount = 1,
            healthyCount = 1,
            newGrowthCount = 1,
        ),
        recentlyAddedPlants = WaterMePreviewData.plants,
        progressStats = DashboardProgressUiModel(
            completedToday = 4,
            dueToday = 3,
            overdue = 1,
            completionPercent = 0.57f,
        ),
        plantCount = WaterMePreviewData.plants.size,
        reminderCount = WaterMePreviewData.reminders.size,
        careHistoryCount = 8,
        noteCount = WaterMePreviewData.healthNotes.size + WaterMePreviewData.plants.count { it.notes.isNotBlank() },
        plantScannerScanCount = 6,
        aiCareUsageCount = 4,
        appOpenDayStreak = 5,
        completedThisWeek = 12,
    )
