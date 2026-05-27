package com.hotelski.waterme.feature.plantdetails

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Notifications
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hotelski.waterme.feature.common.CareHistoryUiModel
import com.hotelski.waterme.feature.common.CareTaskCard
import com.hotelski.waterme.feature.common.CareTaskUiModel
import com.hotelski.waterme.feature.common.CareTypeBadge
import com.hotelski.waterme.feature.common.HealthNoteRow
import com.hotelski.waterme.feature.common.HealthNoteUiModel
import com.hotelski.waterme.feature.common.PlantDetailsUiModel
import com.hotelski.waterme.feature.common.PlantPhotoTile
import com.hotelski.waterme.feature.common.ReminderRow
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
import com.hotelski.waterme.ui.theme.CardWhite
import com.hotelski.waterme.ui.theme.Clay
import com.hotelski.waterme.ui.theme.GardenBackground
import com.hotelski.waterme.ui.theme.Ink
import com.hotelski.waterme.ui.theme.LeafGreen
import com.hotelski.waterme.ui.theme.MutedInk
import com.hotelski.waterme.ui.theme.SoftCream
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
    val activeCharacter: PlantCharacterUiModel? = null,
    val isDeleting: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val heartBurstKey: Long = 0L,
) {
    val shouldShowCharacterCelebration: Boolean
        get() = activeCharacter != null && successMessage?.contains("completed", ignoreCase = true) == true
}

sealed interface PlantDetailsEvent {
    data object BackClicked : PlantDetailsEvent
    data object EditClicked : PlantDetailsEvent
    data object DeleteClicked : PlantDetailsEvent
    data object ConfirmDeleteClicked : PlantDetailsEvent
    data object DismissDeleteClicked : PlantDetailsEvent
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
        if (uiState.successMessage != null) {
            item {
                DetailsMessageCard(
                    title = "Saved",
                    message = uiState.successMessage,
                    color = LeafGreen,
                    icon = Icons.Rounded.Check,
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
                openTaskCount = uiState.pendingTasks.size,
            )
        }

        item {
            PlantHealthStatusCard(
                uiState = uiState,
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
                title = "Upcoming reminders",
                subtitle = "${uiState.reminders.count { it.enabled }} active reminders",
                icon = Icons.Rounded.Notifications,
                initiallyExpanded = uiState.reminders.isNotEmpty(),
            ) {
                UpcomingRemindersSection(reminders = uiState.reminders)
            }
        }

        item {
            ExpandableDetailsSection(
                title = "Pending care",
                subtitle = "${uiState.pendingTasks.size} open tasks",
                icon = Icons.Rounded.LocalFlorist,
                initiallyExpanded = uiState.pendingTasks.isNotEmpty(),
            ) {
                PendingCareSection(uiState.pendingTasks, onEvent)
            }
        }

        item {
            ExpandableDetailsSection(
                title = "Notes",
                subtitle = "Plant notes and quick health observations",
                icon = Icons.Rounded.Eco,
            ) {
                NotesSection(
                    plant = plant,
                    healthNotes = uiState.healthNotes,
                    draft = uiState.healthNoteDraft,
                    selectedMood = uiState.selectedHealthMood,
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
    openTaskCount: Int,
) {
    WaterMeCard(modifier = Modifier.animateContentSize()) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(LeafGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                PlantPhotoTile(
                    photoUri = plant.primaryPhotoUri,
                    plantName = plant.name,
                    size = 132.dp,
                )
                StatusPill(
                    label = healthMood.label(),
                    color = healthMood.accentColor(),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = plant.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryPill("${plant.reminderCount} reminders", Modifier.weight(1f))
                SummaryPill("${plant.careHistoryCount} logs", Modifier.weight(1f))
                SummaryPill("$openTaskCount open", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PlantHealthStatusCard(uiState: PlantDetailsUiState) {
    val mood = currentHealthMood(uiState)
    val latestNote = uiState.healthNotes.firstOrNull()
    val message = when {
        uiState.pendingTasks.any { it.isOverdue } -> "This plant has overdue care. Handle the open tasks first."
        latestNote != null -> latestNote.note
        else -> "No recent health concerns logged. Add a note when leaves, soil, or growth changes."
    }

    WaterMeCard(
        modifier = Modifier.animateContentSize(),
        containerColor = SoftCream,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(mood.accentColor().copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Spa, contentDescription = null, tint = mood.accentColor())
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Plant health status",
                    style = MaterialTheme.typography.labelLarge,
                    color = MutedInk,
                )
                Text(
                    text = mood.label(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedInk,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
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
                    tint = Ink,
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
                Text(careType.label(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink)
                Text(
                    text = reminder?.frequencyLabel ?: "No schedule set",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedInk,
                )
                Text(
                    text = reminder?.nextDueLabel ?: "Edit plant to add ${careType.shortLabel().lowercase()} reminders",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (reminder == null) MutedInk else LeafGreen,
                )
            }
            StatusPill(
                label = if (reminder?.enabled == true) "On" else "Off",
                color = if (reminder?.enabled == true) LeafGreen else MutedInk,
            )
        }
    }
}

@Composable
private fun UpcomingRemindersSection(reminders: List<ReminderUiModel>) {
    val activeReminders = reminders.filter { it.enabled }
    if (activeReminders.isEmpty()) {
        WaterMeEmptyState(
            title = "No upcoming reminders",
            message = "Edit this plant to add care reminders.",
            icon = Icons.Rounded.Notifications,
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            activeReminders.forEach { reminder -> ReminderRow(reminder = reminder) }
        }
    }
}

@Composable
private fun PendingCareSection(
    pendingTasks: List<CareTaskUiModel>,
    onEvent: (PlantDetailsEvent) -> Unit,
) {
    if (pendingTasks.isEmpty()) {
        WaterMeEmptyState(
            title = "No pending care",
            message = "This plant is on track. Upcoming reminders will appear here.",
            icon = Icons.Rounded.Check,
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            pendingTasks.forEach { task ->
                CareTaskCard(
                    task = task,
                    onOpenPlant = {},
                    onComplete = { onEvent(PlantDetailsEvent.CompleteTask(task.id)) },
                    onSkip = { onEvent(PlantDetailsEvent.SkipTask(task.id)) },
                    onSnooze = { onEvent(PlantDetailsEvent.SnoozeTask(task.id)) },
                )
            }
        }
    }
}

@Composable
private fun NotesSection(
    plant: PlantDetailsUiModel,
    healthNotes: List<HealthNoteUiModel>,
    draft: String,
    selectedMood: HealthMood,
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
            onEvent = onEvent,
        )

        if (healthNotes.isEmpty()) {
            NotesEmptyState()
        } else {
            Text(
                "Recent health notes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            healthNotes.take(3).forEach { note -> HealthNoteRow(note = note) }
        }
    }
}

@Composable
private fun HealthNoteComposer(
    draft: String,
    selectedMood: HealthMood,
    onEvent: (PlantDetailsEvent) -> Unit,
) {
    WaterMePremiumCard(
        modifier = Modifier.animateContentSize(),
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.36f),
        shape = RoundedCornerShape(30.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                "Log a quick observation",
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
            WaterMePrimaryButton(
                label = "Add note",
                onClick = { onEvent(PlantDetailsEvent.AddHealthNoteClicked) },
                enabled = draft.isNotBlank(),
            )
        }
    }
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
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(entry.dateLabel, style = MaterialTheme.typography.labelMedium, color = LeafGreen)
            }
            if (entry.notes.isNotBlank()) {
                Text(
                    text = entry.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedInk,
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
            Text("Manage plant", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink)
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
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Ink)
                Text(message, style = MaterialTheme.typography.bodySmall, color = MutedInk)
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

private fun scheduleTypes(reminders: List<ReminderUiModel>): List<CareType> =
    (primaryScheduleTypes + reminders.map { it.careType }).distinct()

private val primaryScheduleTypes = listOf(
    CareType.WATERING,
    CareType.FERTILIZING,
    CareType.REPOTTING,
    CareType.PRUNING,
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
    )
