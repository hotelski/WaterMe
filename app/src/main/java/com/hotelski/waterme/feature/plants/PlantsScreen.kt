package com.hotelski.waterme.feature.plants

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hotelski.waterme.feature.common.PlantCardUiModel
import com.hotelski.waterme.feature.common.PlantPhotoTile
import com.hotelski.waterme.feature.common.WaterMeEmptyState
import com.hotelski.waterme.feature.common.WaterMeErrorState
import com.hotelski.waterme.feature.common.WaterMeLoadingState
import com.hotelski.waterme.feature.common.WaterMePreviewData
import com.hotelski.waterme.feature.common.WaterMeTopBar
import com.hotelski.waterme.feature.common.label
import com.hotelski.waterme.ui.theme.CardWhite
import com.hotelski.waterme.ui.theme.Clay
import com.hotelski.waterme.ui.theme.GardenBackground
import com.hotelski.waterme.ui.theme.Ink
import com.hotelski.waterme.ui.theme.LeafGreen
import com.hotelski.waterme.ui.theme.MutedInk
import com.hotelski.waterme.ui.theme.SoftCream
import com.hotelski.waterme.ui.theme.WaterMeTheme

data class PlantsUiState(
    val isLoading: Boolean = false,
    val plants: List<PlantCardUiModel> = emptyList(),
    val searchQuery: String = "",
    val expandedPlantIds: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && plants.isEmpty() && searchQuery.isBlank()
}

sealed interface PlantsEvent {
    data object AddPlantClicked : PlantsEvent
    data object RetryClicked : PlantsEvent
    data class EditPlantClicked(val plantId: String) : PlantsEvent
    data class NotesAndLogsClicked(val plantId: String) : PlantsEvent
    data class SearchQueryChanged(val value: String) : PlantsEvent
}

@Composable
fun PlantsScreen(
    uiState: PlantsUiState,
    onEvent: (PlantsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = GardenBackground,
        topBar = { WaterMeTopBar(title = "My Plants") },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEvent(PlantsEvent.AddPlantClicked) },
                containerColor = LeafGreen,
                contentColor = Color.White,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add plant")
            }
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> WaterMeLoadingState("Loading your plants...", Modifier.padding(innerPadding))
            uiState.errorMessage != null -> Box(Modifier.padding(innerPadding).padding(20.dp)) {
                WaterMeErrorState(uiState.errorMessage, onRetryClick = { onEvent(PlantsEvent.RetryClicked) })
            }

            else -> LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(GardenBackground),
                contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { PlantsListHeader(uiState) }
                item {
                    PlantSearchField(
                        value = uiState.searchQuery,
                        onValueChange = { onEvent(PlantsEvent.SearchQueryChanged(it)) },
                    )
                }

                when {
                    uiState.isEmpty -> item {
                        WaterMeEmptyState(
                            title = "Your plant list is empty",
                            message = "Add a plant to keep its reminder, note, and logs in one place.",
                            icon = Icons.Rounded.LocalFlorist,
                        )
                    }

                    uiState.plants.isEmpty() -> item {
                        WaterMeEmptyState(
                            title = "No matching plants",
                            message = "Try another plant name or clear the search.",
                            icon = Icons.Rounded.Search,
                        )
                    }

                    else -> items(uiState.plants, key = { plant -> plant.id }) { plant ->
                        PlantListRow(
                            plant = plant,
                            isExpanded = plant.id in uiState.expandedPlantIds,
                            onEdit = { onEvent(PlantsEvent.EditPlantClicked(plant.id)) },
                            onNotesAndLogs = { onEvent(PlantsEvent.NotesAndLogsClicked(plant.id)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlantsListHeader(
    uiState: PlantsUiState,
    modifier: Modifier = Modifier,
) {
    val dueTodayCount = uiState.plants.sumOf { it.dueTaskCount }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SoftCream,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Plant list",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
                Text(
                    text = "${uiState.plants.size} plants organized by reminder and care log",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedInk,
                )
            }
            StatusPill(
                label = "$dueTodayCount due",
                color = if (dueTodayCount > 0) Clay else LeafGreen,
            )
        }
    }
}

@Composable
private fun PlantSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text("Search plants") },
        singleLine = true,
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        shape = RoundedCornerShape(18.dp),
    )
}

@Composable
private fun PlantListRow(
    plant: PlantCardUiModel,
    isExpanded: Boolean,
    onEdit: () -> Unit,
    onNotesAndLogs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(22.dp),
        color = CardWhite,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlantPhotoTile(
                    photoUri = plant.photoUri,
                    plantName = plant.name,
                    size = 58.dp,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = plant.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (plant.dueTaskCount > 0) {
                        StatusPill("${plant.dueTaskCount} due today", Clay)
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit ${plant.name}",
                        tint = LeafGreen,
                    )
                }
            }

            Text(
                text = plant.scheduleSummary,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = MutedInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            OutlinedButton(
                onClick = onNotesAndLogs,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.AutoMirrored.Rounded.Notes, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (isExpanded) "Hide note + logs" else "Show note + ${plant.careLogCount} logs")
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                PlantNotesAndLogs(plant = plant)
            }
        }
    }
}

@Composable
private fun PlantNotesAndLogs(
    plant: PlantCardUiModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SoftCream, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Note",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = LeafGreen,
        )
        Text(
            text = plant.notes.ifBlank { "No note added yet." },
            style = MaterialTheme.typography.bodyMedium,
            color = Ink,
        )

        Text(
            text = "Recent logs",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = LeafGreen,
        )
        if (plant.recentCareLogs.isEmpty()) {
            Text(
                text = "No care logs yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedInk,
            )
        } else {
            plant.recentCareLogs.forEach { log ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "${log.dateLabel} - ${log.careType.label()} - ${log.actionLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink,
                    )
                    if (log.notes.isNotBlank()) {
                        Text(
                            text = log.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedInk,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
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
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.13f),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlantsScreenPreview() {
    WaterMeTheme {
        PlantsScreen(
            uiState = PlantsUiState(plants = WaterMePreviewData.plants),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlantsEmptyPreview() {
    WaterMeTheme {
        PlantsScreen(
            uiState = PlantsUiState(),
            onEvent = {},
        )
    }
}
