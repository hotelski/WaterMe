package com.hotelski.waterme.feature.history

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hotelski.waterme.feature.common.CareHistoryUiModel
import com.hotelski.waterme.feature.common.CareTypeBadge
import com.hotelski.waterme.feature.common.WaterMeEmptyState
import com.hotelski.waterme.feature.common.WaterMeErrorState
import com.hotelski.waterme.feature.common.WaterMeFloatingActionButton
import com.hotelski.waterme.feature.common.WaterMeIconBadge
import com.hotelski.waterme.feature.common.WaterMeLoadingState
import com.hotelski.waterme.feature.common.WaterMePremiumCard
import com.hotelski.waterme.feature.common.WaterMePreviewData
import com.hotelski.waterme.feature.common.WaterMeTopBar
import com.hotelski.waterme.feature.common.accentColor
import com.hotelski.waterme.feature.common.label
import com.hotelski.waterme.feature.common.shortLabel
import com.hotelski.waterme.model.CareType
import com.hotelski.waterme.ui.theme.Clay
import com.hotelski.waterme.ui.theme.LeafGreen
import com.hotelski.waterme.ui.theme.WaterMeTheme

@Composable
fun CareHistoryScreen(
    uiState: CareHistoryUiState,
    onEvent: (CareHistoryEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            WaterMeTopBar(
                title = "Care History",
                navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
                navigationContentDescription = "Back",
                onNavigationClick = { onEvent(CareHistoryEvent.BackClicked) },
                actionIcon = Icons.Rounded.Add,
                actionContentDescription = "Log care",
                onActionClick = { onEvent(CareHistoryEvent.AddManualEntryClicked) },
            )
        },
        floatingActionButton = {
            WaterMeFloatingActionButton(
                onClick = { onEvent(CareHistoryEvent.AddManualEntryClicked) },
                icon = Icons.Rounded.Add,
                contentDescription = "Log care",
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> WaterMeLoadingState("Loading care history...", Modifier.padding(innerPadding))
            uiState.errorMessage != null && uiState.entries.isEmpty() -> Box(Modifier.padding(innerPadding).padding(20.dp)) {
                WaterMeErrorState(uiState.errorMessage, onRetryClick = { onEvent(CareHistoryEvent.RetryClicked) })
            }

            else -> CareHistoryContent(
                uiState = uiState,
                onEvent = onEvent,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }

    if (uiState.draft.isVisible) {
        CareHistoryDraftDialog(
            uiState = uiState,
            onEvent = onEvent,
        )
    }

    if (uiState.pendingDeleteEntryId != null) {
        DeleteCareHistoryDialog(
            isDeleting = uiState.isDeleting,
            onDismiss = { onEvent(CareHistoryEvent.DismissDeleteClicked) },
            onConfirm = { onEvent(CareHistoryEvent.ConfirmDeleteClicked) },
        )
    }
}

@Composable
private fun CareHistoryContent(
    uiState: CareHistoryUiState,
    onEvent: (CareHistoryEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            CareHistorySummaryCard(
                uiState = uiState,
                onLogCare = { onEvent(CareHistoryEvent.AddManualEntryClicked) },
            )
        }
        item {
            CareHistoryFilters(
                uiState = uiState,
                onEvent = onEvent,
            )
        }
        if (uiState.successMessage != null || uiState.errorMessage != null) {
            item {
                CareHistoryMessage(
                    successMessage = uiState.successMessage,
                    errorMessage = uiState.errorMessage,
                )
            }
        }
        if (uiState.isEmpty) {
            item {
                WaterMeEmptyState(
                    title = if (uiState.hasActiveFilters) "No matching entries" else "No care history yet",
                    message = if (uiState.hasActiveFilters) {
                        "Adjust the plant, care type, or date filters to find more care logs."
                    } else {
                        "Log watering, fertilizing, repotting, misting, or pruning after you care for a plant."
                    },
                    icon = Icons.Rounded.History,
                    actionLabel = if (uiState.hasActiveFilters) "Clear filters" else "Log care",
                    onActionClick = {
                        if (uiState.hasActiveFilters) {
                            onEvent(CareHistoryEvent.ClearFiltersClicked)
                        } else {
                            onEvent(CareHistoryEvent.AddManualEntryClicked)
                        }
                    },
                )
            }
        } else {
            val groupedEntries = uiState.entries.groupBy { it.dateLabel }
            groupedEntries.forEach { (dateLabel, entries) ->
                item(key = "date-$dateLabel") {
                    TimelineDateHeader(dateLabel = dateLabel)
                }
                items(entries, key = { it.id }) { entry ->
                    TimelineCareHistoryItem(
                        entry = entry,
                        onEdit = { onEvent(CareHistoryEvent.EditEntryClicked(entry)) },
                        onDelete = { onEvent(CareHistoryEvent.DeleteEntryClicked(entry.id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CareHistorySummaryCard(
    uiState: CareHistoryUiState,
    onLogCare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WaterMePremiumCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f),
        shape = RoundedCornerShape(32.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Plant care journal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Completed tasks, manual logs, notes, and care photos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FilledTonalButton(onClick = onLogCare) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Log care")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SummaryPill("${uiState.entries.size}", "entries", Modifier.weight(1f))
                SummaryPill(uiState.selectedPlantLabel, "plant", Modifier.weight(1f))
                SummaryPill(uiState.selectedDateRange.label, "range", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryPill(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = LeafGreen.copy(alpha = 0.10f),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = LeafGreen,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CareHistoryFilters(
    uiState: CareHistoryUiState,
    onEvent: (CareHistoryEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    WaterMePremiumCard(
        modifier = modifier.animateContentSize(),
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WaterMeIconBadge(icon = Icons.Rounded.FilterList, size = 42.dp, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Filters",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (uiState.hasActiveFilters) {
                    TextButton(onClick = { onEvent(CareHistoryEvent.ClearFiltersClicked) }) {
                        Text("Clear")
                    }
                }
            }

            FilterRow(title = "Plant") {
                FilterChip(
                    selected = uiState.selectedPlantId == null,
                    onClick = { onEvent(CareHistoryEvent.PlantFilterSelected(null)) },
                    label = { Text("All plants") },
                )
                uiState.plantOptions.forEach { plant ->
                    FilterChip(
                        selected = uiState.selectedPlantId == plant.id,
                        onClick = { onEvent(CareHistoryEvent.PlantFilterSelected(plant.id)) },
                        label = { Text(plant.name) },
                    )
                }
            }

            FilterRow(title = "Care type") {
                FilterChip(
                    selected = uiState.selectedCareType == null,
                    onClick = { onEvent(CareHistoryEvent.CareTypeFilterSelected(null)) },
                    label = { Text("All") },
                )
                CareType.entries.forEach { type ->
                    FilterChip(
                        selected = uiState.selectedCareType == type,
                        onClick = { onEvent(CareHistoryEvent.CareTypeFilterSelected(type)) },
                        label = { Text(type.label()) },
                    )
                }
            }

            FilterRow(title = "Date range") {
                CareHistoryDateRange.entries.forEach { range ->
                    FilterChip(
                        selected = uiState.selectedDateRange == range,
                        onClick = { onEvent(CareHistoryEvent.DateRangeSelected(range)) },
                        label = { Text(range.label) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun CareHistoryMessage(
    successMessage: String?,
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
    val message = successMessage ?: errorMessage ?: return
    val color = if (successMessage != null) LeafGreen else Clay
    WaterMePremiumCard(
        modifier = modifier,
        containerColor = color.copy(alpha = 0.10f),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (successMessage != null) Icons.Rounded.Check else Icons.Rounded.Close,
                contentDescription = null,
                tint = color,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun TimelineDateHeader(
    dateLabel: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
        )
    }
}

@Composable
private fun TimelineCareHistoryItem(
    entry: CareHistoryUiModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TimelineMarker(entry.careType)
        WaterMePremiumCard(
            modifier = Modifier
                .weight(1f)
                .animateContentSize(),
            shape = RoundedCornerShape(26.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${entry.actionLabel} ${entry.careType.shortLabel().lowercase()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = entry.plantName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit entry", tint = LeafGreen)
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete entry", tint = Clay)
                        }
                    }
                }
                if (entry.notes.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.Notes, contentDescription = null, tint = LeafGreen, modifier = Modifier.size(18.dp))
                        Text(
                            text = entry.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                if (!entry.photoUri.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = LeafGreen.copy(alpha = 0.10f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Image, contentDescription = null, tint = LeafGreen, modifier = Modifier.size(18.dp))
                            Text("Photo attached", style = MaterialTheme.typography.labelLarge, color = LeafGreen)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineMarker(
    careType: CareType,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(42.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(careType.accentColor().copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            CareTypeBadge(careType, size = 30.dp)
        }
    }
}

@Composable
private fun CareHistoryDraftDialog(
    uiState: CareHistoryUiState,
    onEvent: (CareHistoryEvent) -> Unit,
) {
    val draft = uiState.draft
    AlertDialog(
        onDismissRequest = { onEvent(CareHistoryEvent.DismissDraftClicked) },
        confirmButton = {
            Button(
                onClick = { onEvent(CareHistoryEvent.SaveDraftClicked) },
                enabled = !uiState.isSaving && draft.plantId.isNotBlank(),
            ) {
                Text(if (draft.isEditing) "Update entry" else "Save entry")
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(CareHistoryEvent.DismissDraftClicked) }) {
                Text("Cancel")
            }
        },
        title = {
            Text(if (draft.isEditing) "Edit care entry" else "Log care")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                DialogChipSection(title = "Plant") {
                    uiState.plantOptions.forEach { plant ->
                        FilterChip(
                            selected = draft.plantId == plant.id,
                            onClick = { onEvent(CareHistoryEvent.DraftPlantSelected(plant.id)) },
                            label = { Text(plant.name) },
                        )
                    }
                }
                DialogChipSection(title = "Care type") {
                    CareType.entries.forEach { type ->
                        FilterChip(
                            selected = draft.careType == type,
                            onClick = { onEvent(CareHistoryEvent.DraftCareTypeSelected(type)) },
                            label = { Text(type.label()) },
                        )
                    }
                }
                DialogChipSection(title = "When") {
                    CareHistoryPerformedAtOption.entries.forEach { option ->
                        FilterChip(
                            selected = draft.performedAtOption == option,
                            onClick = { onEvent(CareHistoryEvent.DraftPerformedAtSelected(option)) },
                            label = { Text(option.label) },
                        )
                    }
                }
                OutlinedTextField(
                    value = draft.notes,
                    onValueChange = { onEvent(CareHistoryEvent.DraftNotesChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Notes") },
                    placeholder = { Text("Yellow leaves, dry soil, new growth...") },
                    minLines = 3,
                )
                OutlinedButton(
                    onClick = { onEvent(CareHistoryEvent.DraftPhotoPickerClicked) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (draft.photoUri.isBlank()) "Attach photo" else "Change photo")
                }
                OutlinedTextField(
                    value = draft.photoUri,
                    onValueChange = { onEvent(CareHistoryEvent.DraftPhotoUriChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Photo URI") },
                    placeholder = { Text("Optional plant care photo link") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Image, contentDescription = null) },
                )
                if (draft.errorMessage != null) {
                    Text(
                        text = draft.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = Clay,
                    )
                }
            }
        },
    )
}

@Composable
private fun DialogChipSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.8f))
    }
}

@Composable
private fun DeleteCareHistoryDialog(
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = Clay) },
        title = { Text("Delete care entry?") },
        text = { Text("This removes the entry from the plant's care history timeline.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting,
            ) {
                Text("Delete entry")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isDeleting) {
                Text("Keep entry")
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun CareHistoryScreenPreview() {
    WaterMeTheme {
        CareHistoryScreen(
            uiState = CareHistoryUiState(
                plantOptions = WaterMePreviewData.plants.map { PlantFilterUiModel(it.id, it.name) },
                entries = WaterMePreviewData.history,
            ),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CareHistoryEmptyPreview() {
    WaterMeTheme {
        CareHistoryScreen(
            uiState = CareHistoryUiState(
                plantOptions = WaterMePreviewData.plants.map { PlantFilterUiModel(it.id, it.name) },
            ),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CareHistoryDraftPreview() {
    WaterMeTheme {
        CareHistoryScreen(
            uiState = CareHistoryUiState(
                plantOptions = WaterMePreviewData.plants.map { PlantFilterUiModel(it.id, it.name) },
                entries = WaterMePreviewData.history,
                draft = CareHistoryDraftUiState(
                    isVisible = true,
                    plantId = WaterMePreviewData.plants.first().id,
                    notes = "Dry soil and two new leaves.",
                ),
            ),
            onEvent = {},
        )
    }
}
