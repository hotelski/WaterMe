package com.hotelski.waterme.feature.plants

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hotelski.waterme.feature.common.PlantCardUiModel
import com.hotelski.waterme.feature.common.PlantPhotoTile
import com.hotelski.waterme.feature.common.WaterMeEmptyState
import com.hotelski.waterme.feature.common.WaterMeErrorState
import com.hotelski.waterme.feature.common.WaterMeFloatingActionButton
import com.hotelski.waterme.feature.common.WaterMeIconBadge
import com.hotelski.waterme.feature.common.WaterMeLoadingState
import com.hotelski.waterme.feature.common.WaterMePremiumCard
import com.hotelski.waterme.feature.common.WaterMePreviewData
import com.hotelski.waterme.feature.common.WaterMeStatusChip
import com.hotelski.waterme.feature.common.WaterMeTopBar
import com.hotelski.waterme.feature.common.label
import com.hotelski.waterme.ui.theme.Clay
import com.hotelski.waterme.ui.theme.LeafGreen
import com.hotelski.waterme.ui.theme.MistBlue
import com.hotelski.waterme.ui.theme.WaterMeTheme
import kotlin.math.roundToInt

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
    val fabScale by animateFloatAsState(
        targetValue = if (uiState.plants.isEmpty()) 1.06f else 1f,
        animationSpec = tween(durationMillis = 420),
        label = "plantsFabScale",
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { WaterMeTopBar(title = "My Plants") },
        floatingActionButton = {
            WaterMeFloatingActionButton(
                onClick = { onEvent(PlantsEvent.AddPlantClicked) },
                icon = Icons.Rounded.Add,
                contentDescription = "Add plant",
                modifier = Modifier.graphicsLayer {
                    scaleX = fabScale
                    scaleY = fabScale
                },
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> WaterMeLoadingState("Loading your plants...", Modifier.padding(innerPadding))
            uiState.errorMessage != null && uiState.plants.isEmpty() -> Box(Modifier.padding(innerPadding).padding(20.dp)) {
                WaterMeErrorState(uiState.errorMessage, onRetryClick = { onEvent(PlantsEvent.RetryClicked) })
            }

            else -> PlantsContent(
                uiState = uiState,
                onEvent = onEvent,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun PlantsContent(
    uiState: PlantsUiState,
    onEvent: (PlantsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { PlantsListHeader(uiState) }
        item {
            PlantSearchField(
                value = uiState.searchQuery,
                onValueChange = { onEvent(PlantsEvent.SearchQueryChanged(it)) },
            )
        }
        if (uiState.errorMessage != null) {
            item {
                PlantsInlineMessage(
                    message = uiState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        when {
            uiState.isEmpty -> item {
                WaterMeEmptyState(
                    title = "Your plant list is empty",
                    message = "Add a plant to keep reminders, notes, and care history in one calm place.",
                    icon = Icons.Rounded.LocalFlorist,
                    actionLabel = "Add plant",
                    onActionClick = { onEvent(PlantsEvent.AddPlantClicked) },
                )
            }

            uiState.plants.isEmpty() -> item {
                WaterMeEmptyState(
                    title = "No matching plants",
                    message = "Try another plant name, location, or type.",
                    icon = Icons.Rounded.Search,
                )
            }

            else -> items(uiState.plants, key = { plant -> plant.id }) { plant ->
                SwipePlantCard(
                    plant = plant,
                    isExpanded = plant.id in uiState.expandedPlantIds,
                    onEdit = { onEvent(PlantsEvent.EditPlantClicked(plant.id)) },
                    onNotesAndLogs = { onEvent(PlantsEvent.NotesAndLogsClicked(plant.id)) },
                )
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
    val loggedCount = uiState.plants.sumOf { it.careLogCount }

    WaterMePremiumCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f),
        accentColor = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(32.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Your indoor garden",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Reminders, notes, and care rhythm at a glance.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                WaterMeIconBadge(
                    icon = Icons.Rounded.LocalFlorist,
                    size = 58.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeaderMetric(
                    value = uiState.plants.size.toString(),
                    label = "plants",
                    modifier = Modifier.weight(1f),
                )
                HeaderMetric(
                    value = dueTodayCount.toString(),
                    label = "due today",
                    modifier = Modifier.weight(1f),
                    color = if (dueTodayCount > 0) Clay else LeafGreen,
                )
                HeaderMetric(
                    value = loggedCount.toString(),
                    label = "logs",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HeaderMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
        placeholder = { Text("Search by name, room, or type") },
        singleLine = true,
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
        ),
    )
}

@Composable
private fun PlantsInlineMessage(
    message: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    WaterMePremiumCard(
        modifier = modifier,
        containerColor = color.copy(alpha = 0.08f),
        accentColor = color,
        shape = RoundedCornerShape(22.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SwipePlantCard(
    plant: PlantCardUiModel,
    isExpanded: Boolean,
    onEdit: () -> Unit,
    onNotesAndLogs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actionWidth = 132.dp
    val actionWidthPx = with(LocalDensity.current) { actionWidth.toPx() }
    var rawOffset by remember(plant.id) { mutableFloatStateOf(0f) }
    val offset by animateFloatAsState(
        targetValue = rawOffset,
        animationSpec = tween(durationMillis = 220),
        label = "plantSwipeOffset",
    )

    Box(modifier = modifier.fillMaxWidth()) {
        SwipeActions(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(actionWidth),
            onEdit = {
                rawOffset = 0f
                onEdit()
            },
            onNotesAndLogs = {
                rawOffset = 0f
                onNotesAndLogs()
            },
        )
        PlantListCard(
            plant = plant,
            isExpanded = isExpanded,
            onEdit = onEdit,
            onNotesAndLogs = onNotesAndLogs,
            modifier = Modifier
                .offset { IntOffset(offset.roundToInt(), 0) }
                .pointerInput(plant.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            rawOffset = if (rawOffset < -actionWidthPx * 0.42f) {
                                -actionWidthPx
                            } else {
                                0f
                            }
                        },
                        onDragCancel = { rawOffset = 0f },
                    ) { change, dragAmount ->
                        change.consume()
                        rawOffset = (rawOffset + dragAmount).coerceIn(-actionWidthPx, 0f)
                    }
                },
        )
    }
}

@Composable
private fun SwipeActions(
    onEdit: () -> Unit,
    onNotesAndLogs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onNotesAndLogs) {
            Icon(
                Icons.AutoMirrored.Rounded.Notes,
                contentDescription = "Show notes and logs",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Rounded.Edit, contentDescription = "Edit plant", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun PlantListCard(
    plant: PlantCardUiModel,
    isExpanded: Boolean,
    onEdit: () -> Unit,
    onNotesAndLogs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WaterMePremiumCard(
        modifier = modifier.animateContentSize(),
        accentColor = if (plant.dueTaskCount > 0) Clay else MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(30.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlantPhotoTile(
                    photoUri = plant.photoUri,
                    plantName = plant.name,
                    size = 96.dp,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = plant.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${plant.plantType} - ${plant.location}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(onClick = onEdit, modifier = Modifier.size(42.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Edit ${plant.name}",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        WaterMeStatusChip(
                            label = if (plant.dueTaskCount > 0) "${plant.dueTaskCount} due" else "On track",
                            color = if (plant.dueTaskCount > 0) Clay else LeafGreen,
                            icon = Icons.Rounded.WaterDrop,
                        )
                        if (plant.nextCareLabel != null) {
                            WaterMeStatusChip(
                                label = plant.nextCareLabel,
                                color = MistBlue,
                                icon = Icons.Rounded.Schedule,
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.09f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f),
                            ),
                        ),
                    )
                    .padding(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "Care rhythm",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = plant.scheduleSummary,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    WaterMeStatusChip(
                        label = "${plant.careLogCount} logs",
                        color = MaterialTheme.colorScheme.primary,
                        icon = Icons.Rounded.History,
                    )
                }
            }

            OutlinedButton(
                onClick = onNotesAndLogs,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
            ) {
                Icon(Icons.AutoMirrored.Rounded.Notes, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (isExpanded) "Hide notes and logs" else "Show notes and ${plant.careLogCount} logs")
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(tween(180)) + expandVertically(tween(220)),
                exit = fadeOut(tween(140)) + shrinkVertically(tween(180)),
            ) {
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
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = "Notes",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = plant.notes.ifBlank { "No note added yet." },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Recent care",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            if (plant.recentCareLogs.isEmpty()) {
                Text(
                    text = "No care logs yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                plant.recentCareLogs.forEach { log ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .padding(top = 3.dp)
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "${log.dateLabel} - ${log.careType.label()} - ${log.actionLabel}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (log.notes.isNotBlank()) {
                                Text(
                                    text = log.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
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
