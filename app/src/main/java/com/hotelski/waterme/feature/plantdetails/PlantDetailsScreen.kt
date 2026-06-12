package com.hotelski.waterme.feature.plantdetails

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hotelski.waterme.feature.common.CareHistoryUiModel
import com.hotelski.waterme.feature.common.CareTaskUiModel
import com.hotelski.waterme.feature.common.CareTypeBadge
import com.hotelski.waterme.feature.common.HealthNoteUiModel
import com.hotelski.waterme.feature.common.PlantDetailsUiModel
import com.hotelski.waterme.feature.common.PlantPhotoTile
import com.hotelski.waterme.feature.common.ReminderUiModel
import com.hotelski.waterme.feature.common.WaterMeCard
import com.hotelski.waterme.feature.common.WaterMeEmptyState
import com.hotelski.waterme.feature.common.WaterMeErrorState
import com.hotelski.waterme.feature.common.WaterMeFloatingActionButton
import com.hotelski.waterme.feature.common.WaterMeLoadingState
import com.hotelski.waterme.feature.common.WaterMePremiumCard
import com.hotelski.waterme.feature.common.WaterMePreviewData
import com.hotelski.waterme.feature.common.WaterMePrimaryButton
import com.hotelski.waterme.feature.common.WaterMeTopBar
import com.hotelski.waterme.feature.common.accentColor
import com.hotelski.waterme.feature.common.label
import com.hotelski.waterme.feature.common.shortLabel
import com.hotelski.waterme.feature.characters.PlantCharacterCelebrationCard
import com.hotelski.waterme.feature.characters.PlantCharacterUiModel
import com.hotelski.waterme.model.CareType
import com.hotelski.waterme.model.HealthMood
import com.hotelski.waterme.model.PlantEnvironment
import com.hotelski.waterme.ui.theme.Clay
import com.hotelski.waterme.ui.theme.LeafGreen
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
    val healthNotePhotoUri: String? = null,
    val healthNoteDateTimeLabel: String = "",
    val editingHealthNoteId: String? = null,
    val activeCharacter: PlantCharacterUiModel? = null,
    val isDeleting: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val heartBurstKey: Long = 0L,
) {
    val shouldShowCharacterCelebration: Boolean
        get() = activeCharacter != null &&
            successMessage?.contains("Care task completed", ignoreCase = true) == true

    val shouldShowNotesCharacterCelebration: Boolean
        get() = activeCharacter != null && isHealthNoteSuccess

    private val isHealthNoteSuccess: Boolean
        get() = successMessage?.contains("Health note", ignoreCase = true) == true
}

sealed interface PlantDetailsEvent {
    data object BackClicked : PlantDetailsEvent
    data object EditClicked : PlantDetailsEvent
    data object DeleteClicked : PlantDetailsEvent
    data object ConfirmDeleteClicked : PlantDetailsEvent
    data object DismissDeleteClicked : PlantDetailsEvent
    data object ViewAllHistoryClicked : PlantDetailsEvent
    data object AddHealthNoteClicked : PlantDetailsEvent
    data object ChooseHealthNotePhotoClicked : PlantDetailsEvent
    data object RemoveHealthNotePhotoClicked : PlantDetailsEvent
    data object CancelHealthNoteEditClicked : PlantDetailsEvent
    data object RetryClicked : PlantDetailsEvent
    data class CompleteTask(val taskId: String) : PlantDetailsEvent
    data class SkipTask(val taskId: String) : PlantDetailsEvent
    data class SnoozeTask(val taskId: String) : PlantDetailsEvent
    data class EditHealthNoteClicked(
        val noteId: String,
        val note: String,
        val mood: HealthMood,
        val photoUri: String?,
        val performedAtMillis: Long,
    ) : PlantDetailsEvent
    data class DeleteHealthNoteClicked(val noteId: String) : PlantDetailsEvent
    data class HealthNoteChanged(val value: String) : PlantDetailsEvent
    data class HealthNotePhotoSelected(val uri: String?) : PlantDetailsEvent
    data class HealthMoodSelected(val mood: HealthMood) : PlantDetailsEvent
}

@Composable
fun PlantDetailsScreen(
    uiState: PlantDetailsUiState,
    onEvent: (PlantDetailsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.showDeleteConfirmation && uiState.plant != null) {
        DeletePlantDialog(
            plantName = uiState.plant.name,
            onDismiss = { onEvent(PlantDetailsEvent.DismissDeleteClicked) },
            onConfirm = { onEvent(PlantDetailsEvent.ConfirmDeleteClicked) },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            WaterMeTopBar(
                title = uiState.plant?.name ?: "Plant Details",
                navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
                navigationContentDescription = "Back",
                onNavigationClick = { onEvent(PlantDetailsEvent.BackClicked) },
                actionIcon = Icons.Rounded.Edit,
                actionContentDescription = "Edit plant",
                onActionClick = { onEvent(PlantDetailsEvent.EditClicked) },
            )
        },
        floatingActionButton = {
            if (uiState.plant != null) {
                WaterMeFloatingActionButton(
                    onClick = { onEvent(PlantDetailsEvent.AddHealthNoteClicked) },
                    icon = Icons.Rounded.Add,
                    contentDescription = "Add note",
                )
            }
        },
    ) { innerPadding ->
        val blockingError = uiState.errorMessage
        when {
            uiState.isLoading -> WaterMeLoadingState("Loading plant details...", Modifier.padding(innerPadding))
            uiState.plant == null && blockingError != null -> Box(Modifier.padding(innerPadding).padding(20.dp)) {
                WaterMeErrorState(blockingError, onRetryClick = { onEvent(PlantDetailsEvent.RetryClicked) })
            }

            uiState.plant == null -> Box(Modifier.padding(innerPadding).padding(20.dp)) {
                WaterMeEmptyState(
                    title = "Plant not found",
                    message = "This plant may have been deleted or archived.",
                )
            }

            else -> {
                PlantDetailsContent(
                    uiState = uiState,
                    plant = requireNotNull(uiState.plant),
                    onEvent = onEvent,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun PlantDetailsContent(
    uiState: PlantDetailsUiState,
    plant: PlantDetailsUiModel,
    onEvent: (PlantDetailsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (uiState.errorMessage != null) {
            item {
                DetailsMessageCard(
                    title = "Needs attention",
                    message = uiState.errorMessage,
                    color = Clay,
                    icon = Icons.Rounded.Eco,
                )
            }
        }
        if (uiState.shouldShowCharacterCelebration) {
            item {
                PlantCharacterCelebrationCard(
                    character = requireNotNull(uiState.activeCharacter),
                    message = uiState.successMessage.orEmpty(),
                    heartBurstKey = uiState.heartBurstKey.takeIf { it != 0L },
                )
            }
        }

        item {
            PlantHeroCard(
                plant = plant,
                healthMood = currentHealthMood(uiState),
            )
        }

        item {
            ExpandableDetailsSection(
                title = "Care schedules",
                subtitle = "Watering, feeding, repotting, misting, and pruning",
                icon = Icons.Rounded.Event,
            ) {
                CareScheduleSection(reminders = uiState.reminders)
            }
        }

        item {
            ExpandableDetailsSection(
                title = "Notes",
                subtitle = "Plant notes and quick health observations",
                icon = Icons.AutoMirrored.Rounded.Notes,
            ) {
                NotesSection(
                    plant = plant,
                    healthNotes = uiState.healthNotes,
                    draft = uiState.healthNoteDraft,
                    selectedMood = uiState.selectedHealthMood,
                    selectedPhotoUri = uiState.healthNotePhotoUri,
                    dateTimeLabel = uiState.healthNoteDateTimeLabel,
                    editingNoteId = uiState.editingHealthNoteId,
                    character = uiState.activeCharacter,
                    successMessage = uiState.successMessage,
                    heartBurstKey = uiState.heartBurstKey,
                    showCharacterMessage = uiState.shouldShowNotesCharacterCelebration,
                    onEvent = onEvent,
                )
            }
        }

        item {
            ExpandableDetailsSection(
                title = "Care history",
                subtitle = "${uiState.careHistory.size} timeline entries",
                icon = Icons.Rounded.History,
                initiallyExpanded = uiState.careHistory.isNotEmpty(),
            ) {
                CareHistoryTimelineSection(uiState.careHistory, onEvent)
            }
        }

        item {
            ManagePlantCard(
                isDeleting = uiState.isDeleting,
                onEditClick = { onEvent(PlantDetailsEvent.EditClicked) },
                onDeleteClick = { onEvent(PlantDetailsEvent.DeleteClicked) },
            )
        }
    }
}

@Composable
private fun PlantHeroCard(
    plant: PlantDetailsUiModel,
    healthMood: HealthMood,
) {
    val healthColor = healthMood.accentColor()
    var showPhotoPreview by rememberSaveable(plant.id) { mutableStateOf(false) }

    if (showPhotoPreview) {
        PlantPhotoPreviewDialog(
            plant = plant,
            onDismiss = { showPhotoPreview = false },
        )
    }

    WaterMePremiumCard(
        modifier = Modifier.animateContentSize(),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        accentColor = healthColor,
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            val imageShape = RoundedCornerShape(24.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(236.dp)
                    .clip(imageShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f),
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                            ),
                        ),
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                        shape = imageShape,
                    )
                    .clickable { showPhotoPreview = true },
                contentAlignment = Alignment.Center,
            ) {
                PlantPhotoTile(
                    photoUri = plant.primaryPhotoUri,
                    plantName = plant.name,
                    size = 236.dp,
                    fillContainer = true,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.10f),
                                ),
                            ),
                        ),
                )
                DetailsMetaPill(
                    label = plant.environment.detailsLabel(),
                    icon = plant.environment.detailsIcon(),
                    color = LeafGreen,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = plant.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailsMetricTile("${plant.reminderCount}", "reminders", Icons.Rounded.Event, LeafGreen, Modifier.weight(1f))
                DetailsMetricTile("${plant.careHistoryCount}", "logs", Icons.Rounded.History, Clay, Modifier.weight(1f))
            }

            if (plant.notes.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f))
                        .padding(14.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Care notes",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = LeafGreen,
                        )
                        Text(
                            text = plant.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlantPhotoPreviewDialog(
    plant: PlantDetailsUiModel,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        title = {
            Text(
                text = plant.name,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                PlantPhotoTile(
                    photoUri = plant.primaryPhotoUri,
                    plantName = plant.name,
                    size = 340.dp,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(30.dp),
    )
}

@Composable
private fun DetailsMetaPill(
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .border(1.dp, Color.White.copy(alpha = 0.74f), RoundedCornerShape(999.dp))
            .padding(horizontal = 9.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DetailsMetricTile(
    value: String,
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
            .padding(horizontal = 11.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(color.copy(alpha = 0.11f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun PlantEnvironment.detailsLabel(): String =
    when (this) {
        PlantEnvironment.INDOOR -> "Indoor"
        PlantEnvironment.OUTDOOR -> "Outdoor"
    }

private fun PlantEnvironment.detailsIcon(): ImageVector =
    when (this) {
        PlantEnvironment.INDOOR -> Icons.Rounded.Home
        PlantEnvironment.OUTDOOR -> Icons.Rounded.Park
    }

@Composable
private fun ExpandableDetailsSection(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(LeafGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = LeafGreen)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun CareScheduleSection(reminders: List<ReminderUiModel>) {
    val scheduleTypes = scheduleTypes(reminders)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        scheduleTypes.forEach { careType ->
            CareScheduleCard(
                careType = careType,
                reminder = reminders.firstOrNull { it.careType == careType },
            )
        }
    }
}

@Composable
private fun CareScheduleCard(
    careType: CareType,
    reminder: ReminderUiModel?,
) {
    WaterMeCard(modifier = Modifier.animateContentSize()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CareTypeBadge(careType)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(careType.label(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = reminder?.frequencyLabel ?: "No schedule set",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = reminder?.nextDueLabel ?: "Edit plant to add ${careType.shortLabel().lowercase()} reminders",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (reminder == null) MaterialTheme.colorScheme.onSurfaceVariant else LeafGreen,
                )
            }
            StatusPill(
                label = if (reminder?.enabled == true) "On" else "Off",
                color = if (reminder?.enabled == true) LeafGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NotesSection(
    plant: PlantDetailsUiModel,
    healthNotes: List<HealthNoteUiModel>,
    draft: String,
    selectedMood: HealthMood,
    selectedPhotoUri: String?,
    dateTimeLabel: String,
    editingNoteId: String?,
    character: PlantCharacterUiModel?,
    successMessage: String?,
    heartBurstKey: Long,
    showCharacterMessage: Boolean,
    onEvent: (PlantDetailsEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        WaterMePremiumCard(modifier = Modifier.animateContentSize(), shape = RoundedCornerShape(28.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Plant notes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = plant.notes.ifBlank { "No notes yet. Use Edit plant to add care preferences, light needs, or soil details." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        HealthNoteComposer(
            draft = draft,
            selectedMood = selectedMood,
            selectedPhotoUri = selectedPhotoUri,
            dateTimeLabel = dateTimeLabel,
            isEditing = editingNoteId != null,
            onEvent = onEvent,
        )

        if (showCharacterMessage && character != null && successMessage != null) {
            PlantCharacterCelebrationCard(
                character = character,
                message = successMessage,
                heartBurstKey = heartBurstKey.takeIf { it != 0L },
            )
        }

        if (healthNotes.isEmpty()) {
            NotesEmptyState()
        } else {
            Text(
                "Recent health notes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            healthNotes.take(3).forEach { note ->
                EditableHealthNoteRow(
                    note = note,
                    onEdit = {
                        onEvent(
                            PlantDetailsEvent.EditHealthNoteClicked(
                                noteId = note.id,
                                note = note.note,
                                mood = note.mood,
                                photoUri = note.photoUri,
                                performedAtMillis = note.performedAtMillis,
                            ),
                        )
                    },
                    onDelete = { onEvent(PlantDetailsEvent.DeleteHealthNoteClicked(note.id)) },
                )
            }
        }
    }
}

@Composable
private fun HealthNoteComposer(
    draft: String,
    selectedMood: HealthMood,
    selectedPhotoUri: String?,
    dateTimeLabel: String,
    isEditing: Boolean,
    onEvent: (PlantDetailsEvent) -> Unit,
) {
    WaterMePremiumCard(
        modifier = Modifier.animateContentSize(),
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.36f),
        shape = RoundedCornerShape(30.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                if (isEditing) "Edit observation" else "Log a quick observation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HealthMood.entries.forEach { mood ->
                    FilterChip(
                        selected = mood == selectedMood,
                        onClick = { onEvent(PlantDetailsEvent.HealthMoodSelected(mood)) },
                        leadingIcon = {
                            Icon(
                                imageVector = mood.icon(),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = mood.accentColor(),
                            )
                        },
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
                shape = RoundedCornerShape(20.dp),
            )
            HealthNotePhotoPickerRow(
                photoUri = selectedPhotoUri,
                onChoosePhoto = { onEvent(PlantDetailsEvent.ChooseHealthNotePhotoClicked) },
                onRemovePhoto = { onEvent(PlantDetailsEvent.RemoveHealthNotePhotoClicked) },
            )
            WaterMePrimaryButton(
                label = if (isEditing) "Save note" else "Add note",
                onClick = { onEvent(PlantDetailsEvent.AddHealthNoteClicked) },
                enabled = draft.isNotBlank(),
            )
            if (isEditing) {
                TextButton(onClick = { onEvent(PlantDetailsEvent.CancelHealthNoteEditClicked) }) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun HealthNotePhotoPickerRow(
    photoUri: String?,
    onChoosePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
            onClick = onChoosePhoto,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
        ) {
            Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (photoUri.isNullOrBlank()) "Add photo" else "Change photo")
        }
        if (!photoUri.isNullOrBlank()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlantPhotoTile(
                    photoUri = photoUri,
                    plantName = "Health note",
                    size = 92.dp,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Photo attached",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "This image will be saved with the note.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = onRemovePhoto) {
                        Text("Remove photo")
                    }
                }
            }
        }
    }
}

@Composable
private fun EditableHealthNoteRow(
    note: HealthNoteUiModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var previewPhotoUri by rememberSaveable(note.id) { mutableStateOf<String?>(null) }

    previewPhotoUri?.let { photoUri ->
        HealthNotePhotoPreviewDialog(
            photoUri = photoUri,
            plantName = note.plantName,
            onDismiss = { previewPhotoUri = null },
        )
    }

    WaterMeCard {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(note.mood.accentColor().copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = note.mood.icon(),
                    contentDescription = null,
                    tint = note.mood.accentColor(),
                    modifier = Modifier.size(22.dp),
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = note.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${note.mood.label()} - ${note.dateTimeLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!note.photoUri.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlantPhotoTile(
                            photoUri = note.photoUri,
                            plantName = note.plantName,
                            modifier = Modifier.clickable { previewPhotoUri = note.photoUri },
                            size = 58.dp,
                        )
                        StatusPill(
                            label = "Photo",
                            color = LeafGreen,
                        )
                    }
                }
            }

            IconButton(onClick = onEdit, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit note", tint = LeafGreen)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Rounded.Delete, contentDescription = "Delete note", tint = Clay)
            }
        }
    }
}

@Composable
private fun HealthNotePhotoPreviewDialog(
    photoUri: String,
    plantName: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        title = {
            Text(
                text = "Health note photo",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                PlantPhotoTile(
                    photoUri = photoUri,
                    plantName = plantName,
                    size = 280.dp,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(30.dp),
    )
}

@Composable
private fun NotesEmptyState() {
    WaterMePremiumCard(
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f),
        shape = RoundedCornerShape(30.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Spa,
                    contentDescription = null,
                    modifier = Modifier.size(38.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = "No health notes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Track yellow leaves, dry soil, pests, or new growth here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CareHistoryTimelineSection(
    careHistory: List<CareHistoryUiModel>,
    onEvent: (PlantDetailsEvent) -> Unit,
) {
    if (careHistory.isEmpty()) {
        WaterMeEmptyState(
            title = "No care logged",
            message = "Complete or skip a task to build this plant's history.",
            icon = Icons.Rounded.History,
        )
    } else {
        CareHistoryTimeline(
            entries = careHistory.take(5),
            onViewAllClick = { onEvent(PlantDetailsEvent.ViewAllHistoryClicked) },
        )
    }
}

@Composable
private fun CareHistoryTimeline(
    entries: List<CareHistoryUiModel>,
    onViewAllClick: () -> Unit,
) {
    WaterMeCard(modifier = Modifier.animateContentSize()) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            entries.forEachIndexed { index, entry ->
                CareTimelineItem(
                    entry = entry,
                    isLast = index == entries.lastIndex,
                )
            }
            TextButton(
                onClick = onViewAllClick,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("View all history")
            }
        }
    }
}

@Composable
private fun CareTimelineItem(
    entry: CareHistoryUiModel,
    isLast: Boolean,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(entry.careType.accentColor()),
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(58.dp)
                        .background(entry.careType.accentColor().copy(alpha = 0.22f)),
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 58.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${entry.actionLabel} ${entry.careType.shortLabel().lowercase()}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(entry.dateLabel, style = MaterialTheme.typography.labelMedium, color = LeafGreen)
            }
            if (entry.notes.isNotBlank()) {
                Text(
                    text = entry.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ManagePlantCard(
    isDeleting: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    WaterMeCard(modifier = Modifier.animateContentSize()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Manage plant", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            OutlinedButton(
                onClick = onEditClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Edit plant")
            }
            OutlinedButton(
                onClick = onDeleteClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isDeleting,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Clay),
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (isDeleting) "Deleting..." else "Delete plant")
            }
        }
    }
}

@Composable
private fun DetailsMessageCard(
    title: String,
    message: String,
    color: Color,
    icon: ImageVector,
) {
    WaterMeCard(
        modifier = Modifier.animateContentSize(),
        containerColor = color.copy(alpha = 0.1f),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StatusPill(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SummaryPill(
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(LeafGreen.copy(alpha = 0.1f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = LeafGreen,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DeletePlantDialog(
    plantName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = Clay) },
        title = { Text("Delete $plantName?") },
        text = { Text("This removes the plant from your list. Existing local care records are kept in the database for consistency.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Clay),
            ) {
                Text("Delete plant")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private fun currentHealthMood(uiState: PlantDetailsUiState): HealthMood =
    when {
        uiState.pendingTasks.any { it.isOverdue } -> HealthMood.ATTENTION
        uiState.healthNotes.firstOrNull() != null -> uiState.healthNotes.first().mood
        else -> HealthMood.HEALTHY
    }

private fun HealthMood.icon(): ImageVector =
    when (this) {
        HealthMood.ATTENTION -> Icons.Rounded.Spa
        HealthMood.HEALTHY -> Icons.Rounded.Check
        HealthMood.GROWTH -> Icons.Rounded.Eco
    }

private fun scheduleTypes(reminders: List<ReminderUiModel>): List<CareType> =
    (primaryScheduleTypes + reminders.map { it.careType }.filter { it in primaryScheduleTypes }).distinct()

private val primaryScheduleTypes = listOf(
    CareType.WATERING,
    CareType.FERTILIZING,
)

@Preview(showBackground = true)
@Composable
private fun PlantDetailsScreenPreview() {
    WaterMeTheme {
        PlantDetailsScreen(
            uiState = plantDetailsPreviewState(),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlantDetailsLoadingPreview() {
    WaterMeTheme {
        PlantDetailsScreen(
            uiState = PlantDetailsUiState(isLoading = true),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlantDetailsEmptyPreview() {
    WaterMeTheme {
        PlantDetailsScreen(
            uiState = PlantDetailsUiState(),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlantDetailsErrorPreview() {
    WaterMeTheme {
        PlantDetailsScreen(
            uiState = PlantDetailsUiState(errorMessage = "WaterMe could not load this plant."),
            onEvent = {},
        )
    }
}

private fun plantDetailsPreviewState(): PlantDetailsUiState =
    PlantDetailsUiState(
        plant = WaterMePreviewData.plantDetails,
        reminders = WaterMePreviewData.reminders,
        pendingTasks = WaterMePreviewData.tasks.take(2),
        careHistory = WaterMePreviewData.history,
        healthNotes = WaterMePreviewData.healthNotes,
        healthNoteDraft = "Two leaves look a little dry near the window.",
        selectedHealthMood = HealthMood.ATTENTION,
        healthNoteDateTimeLabel = "Today, 9:15 AM",
    )
