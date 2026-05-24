package com.hotelski.waterme.feature.plantdetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hotelski.waterme.feature.common.CareHistoryRow
import com.hotelski.waterme.feature.common.CareHistoryUiModel
import com.hotelski.waterme.feature.common.CareTaskCard
import com.hotelski.waterme.feature.common.CareTaskUiModel
import com.hotelski.waterme.feature.common.HealthNoteRow
import com.hotelski.waterme.feature.common.HealthNoteUiModel
import com.hotelski.waterme.feature.common.PlantDetailsUiModel
import com.hotelski.waterme.feature.common.PlantPhotoTile
import com.hotelski.waterme.feature.common.ReminderRow
import com.hotelski.waterme.feature.common.ReminderUiModel
import com.hotelski.waterme.feature.common.WaterMeCard
import com.hotelski.waterme.feature.common.WaterMeEmptyState
import com.hotelski.waterme.feature.common.WaterMeErrorState
import com.hotelski.waterme.feature.common.WaterMeLoadingState
import com.hotelski.waterme.feature.common.WaterMePreviewData
import com.hotelski.waterme.feature.common.WaterMePrimaryButton
import com.hotelski.waterme.feature.common.WaterMeSectionHeader
import com.hotelski.waterme.feature.common.WaterMeTopBar
import com.hotelski.waterme.feature.common.label
import com.hotelski.waterme.model.HealthMood
import com.hotelski.waterme.ui.theme.GardenBackground
import com.hotelski.waterme.ui.theme.Ink
import com.hotelski.waterme.ui.theme.LeafGreen
import com.hotelski.waterme.ui.theme.MutedInk
import com.hotelski.waterme.ui.theme.WaterMeTheme

data class PlantDetailsUiState(
    val isLoading: Boolean = false,
    val plant: PlantDetailsUiModel? = null,
    val reminders: List<ReminderUiModel> = emptyList(),
    val pendingTasks: List<CareTaskUiModel> = emptyList(),
    val careHistory: List<CareHistoryUiModel> = emptyList(),
    val healthNotes: List<HealthNoteUiModel> = emptyList(),
    val healthNoteDraft: String = "",
    val selectedHealthMood: HealthMood = HealthMood.ATTENTION,
    val errorMessage: String? = null,
)

sealed interface PlantDetailsEvent {
    data object BackClicked : PlantDetailsEvent
    data object EditClicked : PlantDetailsEvent
    data object ViewAllHistoryClicked : PlantDetailsEvent
    data object AddHealthNoteClicked : PlantDetailsEvent
    data object RetryClicked : PlantDetailsEvent
    data class CompleteTask(val taskId: String) : PlantDetailsEvent
    data class SkipTask(val taskId: String) : PlantDetailsEvent
    data class SnoozeTask(val taskId: String) : PlantDetailsEvent
    data class HealthNoteChanged(val value: String) : PlantDetailsEvent
    data class HealthMoodSelected(val mood: HealthMood) : PlantDetailsEvent
}

@Composable
fun PlantDetailsScreen(
    uiState: PlantDetailsUiState,
    onEvent: (PlantDetailsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = GardenBackground,
        topBar = {
            WaterMeTopBar(
                title = uiState.plant?.name ?: "Plant Details",
                navigationIcon = Icons.Rounded.ArrowBack,
                navigationContentDescription = "Back",
                onNavigationClick = { onEvent(PlantDetailsEvent.BackClicked) },
                actionIcon = Icons.Rounded.Edit,
                actionContentDescription = "Edit plant",
                onActionClick = { onEvent(PlantDetailsEvent.EditClicked) },
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> WaterMeLoadingState("Loading plant details...", Modifier.padding(innerPadding))
            uiState.errorMessage != null -> Box(Modifier.padding(innerPadding).padding(20.dp)) {
                WaterMeErrorState(uiState.errorMessage, onRetryClick = { onEvent(PlantDetailsEvent.RetryClicked) })
            }

            uiState.plant == null -> Box(Modifier.padding(innerPadding).padding(20.dp)) {
                WaterMeEmptyState(
                    title = "Plant not found",
                    message = "This plant may have been deleted or archived.",
                )
            }

            else -> PlantDetailsContent(
                uiState = uiState,
                onEvent = onEvent,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun PlantDetailsContent(
    uiState: PlantDetailsUiState,
    onEvent: (PlantDetailsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(GardenBackground),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { PlantHeaderCard(uiState.plant!!) }

        item { WaterMeSectionHeader("Pending Care") }
        if (uiState.pendingTasks.isEmpty()) {
            item {
                WaterMeEmptyState(
                    title = "No pending care",
                    message = "This plant is on track. Upcoming reminders will appear here.",
                )
            }
        } else {
            items(uiState.pendingTasks, key = { it.id }) { task ->
                CareTaskCard(
                    task = task,
                    onOpenPlant = {},
                    onComplete = { onEvent(PlantDetailsEvent.CompleteTask(task.id)) },
                    onSkip = { onEvent(PlantDetailsEvent.SkipTask(task.id)) },
                    onSnooze = { onEvent(PlantDetailsEvent.SnoozeTask(task.id)) },
                )
            }
        }

        item { WaterMeSectionHeader("Reminders") }
        if (uiState.reminders.isEmpty()) {
            item {
                WaterMeEmptyState(
                    title = "No reminders",
                    message = "Edit this plant to add watering, feeding, misting, repotting, or pruning reminders.",
                )
            }
        } else {
            items(uiState.reminders, key = { it.id }) { reminder ->
                ReminderRow(reminder = reminder)
            }
        }

        item { WaterMeSectionHeader("Health Notes") }
        item {
            HealthNoteComposer(
                draft = uiState.healthNoteDraft,
                selectedMood = uiState.selectedHealthMood,
                onEvent = onEvent,
            )
        }
        items(uiState.healthNotes, key = { it.id }) { note ->
            HealthNoteRow(note)
        }

        item {
            WaterMeSectionHeader(
                title = "Care History",
                actionLabel = "View all",
                onActionClick = { onEvent(PlantDetailsEvent.ViewAllHistoryClicked) },
            )
        }
        if (uiState.careHistory.isEmpty()) {
            item {
                WaterMeEmptyState(
                    title = "No care logged",
                    message = "Tap Done on a care task to build this plant's history.",
                    icon = Icons.Rounded.History,
                )
            }
        } else {
            items(uiState.careHistory.take(3), key = { it.id }) { entry ->
                CareHistoryRow(entry)
            }
        }
    }
}

@Composable
private fun PlantHeaderCard(plant: PlantDetailsUiModel) {
    WaterMeCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                PlantPhotoTile(plant.primaryPhotoUri, plant.name, size = 104.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(plant.name, style = androidx.compose.material3.MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
                    Text(plant.plantType, color = LeafGreen)
                    Text(plant.location, color = MutedInk)
                }
            }
            if (plant.notes.isNotBlank()) {
                Text(plant.notes, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = MutedInk)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("${plant.reminderCount} reminders") })
                AssistChip(onClick = {}, label = { Text("${plant.careHistoryCount} logs") })
                AssistChip(onClick = {}, label = { Text("${plant.photoCount} photos") })
            }
        }
    }
}

@Composable
private fun HealthNoteComposer(
    draft: String,
    selectedMood: HealthMood,
    onEvent: (PlantDetailsEvent) -> Unit,
) {
    WaterMeCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Log a quick observation", style = androidx.compose.material3.MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HealthMood.entries.forEach { mood ->
                    FilterChip(
                        selected = mood == selectedMood,
                        onClick = { onEvent(PlantDetailsEvent.HealthMoodSelected(mood)) },
                        label = { Text(mood.label()) },
                    )
                }
            }
            OutlinedTextField(
                value = draft,
                onValueChange = { onEvent(PlantDetailsEvent.HealthNoteChanged(it)) },
                label = { Text("Yellow leaves, dry soil, new growth...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            WaterMePrimaryButton(
                label = "Add note",
                onClick = { onEvent(PlantDetailsEvent.AddHealthNoteClicked) },
                enabled = draft.isNotBlank(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlantDetailsScreenPreview() {
    WaterMeTheme {
        PlantDetailsScreen(
            uiState = PlantDetailsUiState(
                plant = WaterMePreviewData.plantDetails,
                reminders = WaterMePreviewData.reminders,
                pendingTasks = WaterMePreviewData.tasks.take(2),
                careHistory = WaterMePreviewData.history,
                healthNotes = WaterMePreviewData.healthNotes,
            ),
            onEvent = {},
        )
    }
}
