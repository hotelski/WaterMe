package com.hotelski.waterme.feature.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hotelski.waterme.feature.common.CareTaskCard
import com.hotelski.waterme.feature.common.CareTaskUiModel
import com.hotelski.waterme.feature.common.HealthNoteRow
import com.hotelski.waterme.feature.common.HealthNoteUiModel
import com.hotelski.waterme.feature.common.WaterMeCard
import com.hotelski.waterme.feature.common.WaterMeEmptyState
import com.hotelski.waterme.feature.common.WaterMeErrorState
import com.hotelski.waterme.feature.common.WaterMeLoadingState
import com.hotelski.waterme.feature.common.WaterMePreviewData
import com.hotelski.waterme.feature.common.WaterMeSectionHeader
import com.hotelski.waterme.feature.common.WaterMeTopBar
import com.hotelski.waterme.ui.theme.CardWhite
import com.hotelski.waterme.ui.theme.GardenBackground
import com.hotelski.waterme.ui.theme.Ink
import com.hotelski.waterme.ui.theme.LeafGreen
import com.hotelski.waterme.ui.theme.MutedInk
import com.hotelski.waterme.ui.theme.WaterMeTheme

data class TodayUiState(
    val isLoading: Boolean = false,
    val tasks: List<CareTaskUiModel> = emptyList(),
    val healthNotes: List<HealthNoteUiModel> = emptyList(),
    val plantCount: Int = 0,
    val reminderCount: Int = 0,
    val errorMessage: String? = null,
    val successMessage: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && tasks.isEmpty()
}

sealed interface TodayEvent {
    data object AddPlantClicked : TodayEvent
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
        topBar = { WaterMeTopBar(title = "Today") },
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
            uiState.isLoading -> WaterMeLoadingState("Checking today's plant care...", Modifier.padding(innerPadding))
            uiState.errorMessage != null -> Box(Modifier.padding(innerPadding).padding(20.dp)) {
                WaterMeErrorState(
                    message = uiState.errorMessage,
                    onRetryClick = { onEvent(TodayEvent.RetryClicked) },
                )
            }

            else -> TodayContent(
                uiState = uiState,
                onEvent = onEvent,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun TodayContent(
    uiState: TodayUiState,
    onEvent: (TodayEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(GardenBackground),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            TodayHeroCard(
                taskCount = uiState.tasks.size,
                plantCount = uiState.plantCount,
                reminderCount = uiState.reminderCount,
            )
        }

        item { WaterMeSectionHeader("Today's Care") }

        if (uiState.isEmpty) {
            item {
                WaterMeEmptyState(
                    title = "No urgent care",
                    message = "Enjoy the quiet. Your next care task is waiting in the calendar.",
                    actionLabel = "Add plant",
                    onActionClick = { onEvent(TodayEvent.AddPlantClicked) },
                )
            }
        } else {
            items(uiState.tasks, key = { it.id }) { task ->
                CareTaskCard(
                    task = task,
                    onOpenPlant = { onEvent(TodayEvent.PlantClicked(task.plantId)) },
                    onComplete = { onEvent(TodayEvent.CompleteTask(task.id)) },
                    onSkip = { onEvent(TodayEvent.SkipTask(task.id)) },
                    onSnooze = { onEvent(TodayEvent.SnoozeTask(task.id)) },
                )
            }
        }

        item { WaterMeSectionHeader("Health Notes") }

        if (uiState.healthNotes.isEmpty()) {
            item {
                WaterMeEmptyState(
                    title = "No notes yet",
                    message = "Use plant details to track yellow leaves, dry soil, fresh growth, or anything worth remembering.",
                )
            }
        } else {
            items(uiState.healthNotes, key = { it.id }) { note ->
                HealthNoteRow(note = note)
            }
        }
    }
}

@Composable
private fun TodayHeroCard(
    taskCount: Int,
    plantCount: Int,
    reminderCount: Int,
) {
    WaterMeCard(containerColor = LeafGreen) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                Icons.Rounded.LocalFlorist,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.padding(top = 2.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (taskCount == 0) "All clear today" else "$taskCount care task${if (taskCount == 1) "" else "s"} today",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = if (taskCount == 0) {
                        "Your plants are on schedule. Check the calendar for what is coming next."
                    } else {
                        "A few plants need attention. Log care as you go and WaterMe will move the schedule forward."
                    },
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.86f),
                )
                Text(
                    text = "$plantCount plants - $reminderCount reminders",
                    modifier = Modifier.padding(top = 14.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = CardWhite,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TodayScreenPreview() {
    WaterMeTheme {
        TodayScreen(
            uiState = TodayUiState(
                tasks = WaterMePreviewData.tasks,
                healthNotes = WaterMePreviewData.healthNotes,
                plantCount = 3,
                reminderCount = 8,
            ),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TodayEmptyPreview() {
    WaterMeTheme {
        TodayScreen(
            uiState = TodayUiState(plantCount = 3, reminderCount = 8),
            onEvent = {},
        )
    }
}
