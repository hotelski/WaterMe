package com.hotelski.waterme.feature.calendar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hotelski.waterme.feature.common.CareHistoryUiModel
import com.hotelski.waterme.feature.common.CareTaskUiModel
import com.hotelski.waterme.feature.common.CareTypeBadge
import com.hotelski.waterme.feature.common.PlantPhotoTile
import com.hotelski.waterme.feature.common.WaterMeEmptyState
import com.hotelski.waterme.feature.common.WaterMeErrorState
import com.hotelski.waterme.feature.common.WaterMeIconBadge
import com.hotelski.waterme.feature.common.WaterMeLoadingState
import com.hotelski.waterme.feature.common.WaterMePremiumCard
import com.hotelski.waterme.feature.common.WaterMePreviewData
import com.hotelski.waterme.feature.common.WaterMeTopBar
import com.hotelski.waterme.feature.common.accentColor
import com.hotelski.waterme.feature.common.icon
import com.hotelski.waterme.feature.common.label
import com.hotelski.waterme.feature.common.shortLabel
import com.hotelski.waterme.feature.characters.PlantCharacterCelebrationCard
import com.hotelski.waterme.feature.characters.PlantCharacterUiModel
import com.hotelski.waterme.ui.theme.Clay
import com.hotelski.waterme.ui.theme.LeafGreen
import com.hotelski.waterme.ui.theme.WaterMeTheme
import kotlinx.coroutines.delay

data class CalendarDateUiModel(
    val dateMillis: Long,
    val weekdayLabel: String,
    val dayOfMonthLabel: String,
    val accessibilityLabel: String,
    val isToday: Boolean = false,
    val isSelected: Boolean = false,
    val hasCare: Boolean = false,
)

data class CalendarPlantFilterUiModel(
    val id: String,
    val name: String,
)

data class CalendarUiState(
    val isLoading: Boolean = false,
    val dayStrip: List<CalendarDateUiModel> = emptyList(),
    val selectedDateMillis: Long = 0L,
    val selectedDateLabel: String = "Today",
    val selectedMonthLabel: String = "",
    val selectedDateIsFuture: Boolean = false,
    val selectedDayTasks: List<CareTaskUiModel> = emptyList(),
    val selectedDayLogs: List<CareHistoryUiModel> = emptyList(),
    val monthlyLogs: List<CareHistoryUiModel> = emptyList(),
    val selectedPlantId: String? = null,
    val plantOptions: List<CalendarPlantFilterUiModel> = emptyList(),
    val activeCharacter: PlantCharacterUiModel? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val heartBurstKey: Long = 0L,
) {
    val selectedPlantLabel: String
        get() = plantOptions.firstOrNull { it.id == selectedPlantId }?.name ?: "All plants"

    val shouldShowCharacterCelebration: Boolean
        get() = activeCharacter != null && successMessage?.contains("completed", ignoreCase = true) == true
}

sealed interface CalendarEvent {
    data object ScreenEntered : CalendarEvent
    data object RetryClicked : CalendarEvent
    data object TodayClicked : CalendarEvent
    data class DateSelected(val dateMillis: Long) : CalendarEvent
    data class PlantFilterSelected(val plantId: String?) : CalendarEvent
    data class CompleteTask(val taskId: String) : CalendarEvent
    data class CharacterCelebrationExpired(val heartBurstKey: Long) : CalendarEvent
    data class TaskClicked(val plantId: String, val taskId: String) : CalendarEvent
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    onEvent: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onEvent(CalendarEvent.ScreenEntered)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { WaterMeTopBar(title = "Calendar") },
    ) { innerPadding ->
        when {
            uiState.isLoading -> WaterMeLoadingState("Loading calendar...", Modifier.padding(innerPadding))
            uiState.errorMessage != null && uiState.dayStrip.isEmpty() -> Box(Modifier.padding(innerPadding).padding(20.dp)) {
                WaterMeErrorState(uiState.errorMessage, onRetryClick = { onEvent(CalendarEvent.RetryClicked) })
            }

            else -> CalendarContent(
                uiState = uiState,
                onEvent = onEvent,
                onOpenCalendar = { showDatePicker = true },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }

    if (showDatePicker) {
        CalendarDatePickerDialog(
            selectedDateMillis = uiState.selectedDateMillis,
            onDismiss = { showDatePicker = false },
            onDateSelected = { millis ->
                showDatePicker = false
                onEvent(CalendarEvent.DateSelected(millis))
            },
        )
    }
}

@Composable
private fun CalendarContent(
    uiState: CalendarUiState,
    onEvent: (CalendarEvent) -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(uiState.heartBurstKey, uiState.shouldShowCharacterCelebration) {
        if (uiState.shouldShowCharacterCelebration && uiState.heartBurstKey != 0L) {
            delay(CHARACTER_CELEBRATION_VISIBLE_MILLIS)
            onEvent(CalendarEvent.CharacterCelebrationExpired(uiState.heartBurstKey))
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            CalendarDayStrip(
                uiState = uiState,
                onTodayClick = { onEvent(CalendarEvent.TodayClicked) },
                onOpenCalendar = onOpenCalendar,
                onDateClick = { onEvent(CalendarEvent.DateSelected(it)) },
            )
        }

        val shouldShowInlineMessage = uiState.errorMessage != null ||
            (uiState.successMessage != null && !uiState.shouldShowCharacterCelebration)
        if (shouldShowInlineMessage) {
            item {
                CalendarMessage(
                    successMessage = uiState.successMessage,
                    errorMessage = uiState.errorMessage,
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
            SelectedDaySection(
                uiState = uiState,
                onOpenPlant = { plantId, taskId -> onEvent(CalendarEvent.TaskClicked(plantId, taskId)) },
                onCompleteTask = { onEvent(CalendarEvent.CompleteTask(it)) },
            )
        }

        item {
            MonthlyLogHeader(uiState = uiState)
        }

        item {
            CalendarPlantFilters(
                uiState = uiState,
                onPlantSelected = { onEvent(CalendarEvent.PlantFilterSelected(it)) },
            )
        }

        if (uiState.monthlyLogs.isEmpty()) {
            item {
                WaterMeEmptyState(
                    title = "No logs this month",
                    message = if (uiState.selectedPlantId == null) {
                        "Completed care and manual logs will appear here for ${uiState.selectedMonthLabel}."
                    } else {
                        "No ${uiState.selectedPlantLabel} logs match this month."
                    },
                    icon = Icons.Rounded.History,
                )
            }
        } else {
            items(uiState.monthlyLogs, key = { it.id }) { entry ->
                CalendarHistoryCard(entry = entry)
            }
        }
    }
}

@Composable
private fun CalendarDayStrip(
    uiState: CalendarUiState,
    onTodayClick: () -> Unit,
    onOpenCalendar: () -> Unit,
    onDateClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    ScrollSelectedDayIntoView(uiState.dayStrip, listState)

    WaterMePremiumCard(
        modifier = modifier.animateContentSize(),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(30.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.selectedMonthLabel,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = uiState.selectedDateLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onTodayClick) {
                    Icon(Icons.Rounded.Today, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Today")
                }
                IconButton(
                    onClick = onOpenCalendar,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.11f)),
                ) {
                    Icon(
                        Icons.Rounded.Event,
                        contentDescription = "Open full calendar",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
            ) {
                items(uiState.dayStrip, key = { it.dateMillis }) { day ->
                    CalendarDayChip(
                        day = day,
                        onClick = { onDateClick(day.dateMillis) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScrollSelectedDayIntoView(
    days: List<CalendarDateUiModel>,
    listState: LazyListState,
) {
    LaunchedEffect(days) {
        val selectedIndex = days.indexOfFirst { it.isSelected }
        if (selectedIndex >= 0) {
            listState.animateScrollToItem((selectedIndex - 2).coerceAtLeast(0))
        }
    }
}

@Composable
private fun CalendarDayChip(
    day: CalendarDateUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = if (day.isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
        },
        label = "calendarDayContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = if (day.isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        label = "calendarDayContent",
    )

    Surface(
        modifier = modifier
            .size(width = 58.dp, height = 76.dp)
            .clip(RoundedCornerShape(22.dp))
            .semantics { contentDescription = day.accessibilityLabel }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = day.weekdayLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                text = day.dayOfMonthLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (day.isToday) {
                    CalendarDot(color = if (day.isSelected) MaterialTheme.colorScheme.onPrimary else LeafGreen)
                }
                if (day.hasCare) {
                    CalendarDot(color = if (day.isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f) else Clay)
                }
            }
        }
    }
}

@Composable
private fun CalendarDot(color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .size(5.dp)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
private fun SelectedDaySection(
    uiState: CalendarUiState,
    onOpenPlant: (String, String) -> Unit,
    onCompleteTask: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionTitle(title = "Day care", subtitle = uiState.selectedDateLabel)

        if (uiState.selectedDayTasks.isEmpty() && uiState.selectedDayLogs.isEmpty()) {
            WaterMeEmptyState(
                title = "No care for this day",
                message = "Scheduled care and completed logs for the selected day will appear here.",
                icon = Icons.Rounded.Event,
            )
        } else {
            uiState.selectedDayTasks.forEach { task ->
                CalendarTaskCard(
                    task = task,
                    completeEnabled = !uiState.selectedDateIsFuture,
                    onOpenPlant = { onOpenPlant(task.plantId, task.id) },
                    onComplete = { onCompleteTask(task.id) },
                )
            }
            uiState.selectedDayLogs.forEach { entry ->
                CalendarHistoryCard(entry = entry)
            }
        }
    }
}

@Composable
private fun MonthlyLogHeader(
    uiState: CalendarUiState,
    modifier: Modifier = Modifier,
) {
    SectionTitle(
        title = "Month logs",
        subtitle = "${uiState.selectedMonthLabel} - ${uiState.selectedPlantLabel}",
        modifier = modifier,
    )
}

@Composable
private fun SectionTitle(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CalendarPlantFilters(
    uiState: CalendarUiState,
    onPlantSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
        item {
            FilterChip(
                selected = uiState.selectedPlantId == null,
                onClick = { onPlantSelected(null) },
                label = { Text("All plants") },
            )
        }
        items(uiState.plantOptions, key = { it.id }) { plant ->
            FilterChip(
                selected = uiState.selectedPlantId == plant.id,
                onClick = { onPlantSelected(plant.id) },
                label = { Text(plant.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
    }
}

@Composable
private fun CalendarTaskCard(
    task: CareTaskUiModel,
    completeEnabled: Boolean,
    onOpenPlant: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WaterMePremiumCard(
        modifier = modifier.clickable(onClick = onOpenPlant),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        accentColor = task.careType.accentColor(),
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 58.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(task.careType.accentColor()),
            )
            Box {
                PlantPhotoTile(
                    photoUri = task.plantPhotoUri,
                    plantName = task.plantName,
                    size = 48.dp,
                )
                Icon(
                    imageVector = task.careType.icon(),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(21.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    tint = task.careType.accentColor(),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = task.careType.label(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = task.plantName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                CalendarTaskStatusPill(task = task)
            }
            Button(
                onClick = onComplete,
                enabled = completeEnabled,
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 13.dp, vertical = 9.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LeafGreen,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.size(6.dp))
                Text("Done", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun CalendarTaskStatusPill(
    task: CareTaskUiModel,
    modifier: Modifier = Modifier,
) {
    val color = when {
        task.isOverdue -> Clay
        task.isSnoozed -> MaterialTheme.colorScheme.secondary
        else -> task.careType.accentColor()
    }
    val label = when {
        task.isOverdue -> "Overdue"
        task.isSnoozed -> "Snoozed"
        else -> task.dueLabel
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.12f),
        contentColor = color,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CalendarHistoryCard(
    entry: CareHistoryUiModel,
    modifier: Modifier = Modifier,
) {
    WaterMePremiumCard(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WaterMeIconBadge(
                icon = Icons.Rounded.History,
                size = 42.dp,
                color = entry.careType.accentColor(),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${entry.actionLabel} ${entry.careType.shortLabel().lowercase()}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = entry.plantName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
            Text(
                text = entry.dateLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = LeafGreen,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CalendarMessage(
    successMessage: String?,
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
    val message = successMessage ?: errorMessage ?: return
    val color = if (successMessage != null) LeafGreen else Clay
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.11f),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = color,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarDatePickerDialog(
    selectedDateMillis: Long,
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit,
) {
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onDateSelected(pickerState.selectedDateMillis ?: selectedDateMillis) },
            ) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    ) {
        DatePicker(state = pickerState)
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarScreenPreview() {
    WaterMeTheme {
        CalendarScreen(
            uiState = CalendarUiState(
                dayStrip = previewDayStrip,
                selectedDateMillis = previewDayStrip.first().dateMillis,
                selectedDateLabel = "Today, May 26",
                selectedMonthLabel = "May 2026",
                selectedDayTasks = WaterMePreviewData.tasks.take(2),
                selectedDayLogs = WaterMePreviewData.history.take(1),
                monthlyLogs = WaterMePreviewData.history,
                plantOptions = WaterMePreviewData.plants.map { CalendarPlantFilterUiModel(it.id, it.name) },
            ),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarEmptyPreview() {
    WaterMeTheme {
        CalendarScreen(
            uiState = CalendarUiState(
                dayStrip = previewDayStrip,
                selectedDateMillis = previewDayStrip.first().dateMillis,
                selectedDateLabel = "Today, May 26",
                selectedMonthLabel = "May 2026",
            ),
            onEvent = {},
        )
    }
}

private val previewDayStrip = listOf(
    CalendarDateUiModel(
        dateMillis = 1L,
        weekdayLabel = "MON",
        dayOfMonthLabel = "25",
        accessibilityLabel = "Monday, May 25",
        hasCare = true,
    ),
    CalendarDateUiModel(
        dateMillis = 2L,
        weekdayLabel = "TUE",
        dayOfMonthLabel = "26",
        accessibilityLabel = "Tuesday, May 26",
        isToday = true,
        isSelected = true,
        hasCare = true,
    ),
    CalendarDateUiModel(
        dateMillis = 3L,
        weekdayLabel = "WED",
        dayOfMonthLabel = "27",
        accessibilityLabel = "Wednesday, May 27",
    ),
)

private const val CHARACTER_CELEBRATION_VISIBLE_MILLIS = 3_200L
