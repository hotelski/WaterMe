package com.hotelski.waterme.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hotelski.waterme.feature.common.CalendarDayUiModel
import com.hotelski.waterme.feature.common.CareTaskCard
import com.hotelski.waterme.feature.common.WaterMeCard
import com.hotelski.waterme.feature.common.WaterMeEmptyState
import com.hotelski.waterme.feature.common.WaterMeErrorState
import com.hotelski.waterme.feature.common.WaterMeLoadingState
import com.hotelski.waterme.feature.common.WaterMePreviewData
import com.hotelski.waterme.feature.common.WaterMeSectionHeader
import com.hotelski.waterme.feature.common.WaterMeTopBar
import com.hotelski.waterme.ui.theme.GardenBackground
import com.hotelski.waterme.ui.theme.WaterMeTheme

data class CalendarUiState(
    val isLoading: Boolean = false,
    val days: List<CalendarDayUiModel> = emptyList(),
    val errorMessage: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && days.all { it.tasks.isEmpty() }
}

sealed interface CalendarEvent {
    data object RetryClicked : CalendarEvent
    data object TodayClicked : CalendarEvent
    data class CompleteTask(val taskId: String) : CalendarEvent
    data class TaskClicked(val plantId: String, val taskId: String) : CalendarEvent
}

@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    onEvent: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = GardenBackground,
        topBar = { WaterMeTopBar(title = "Calendar") },
    ) { innerPadding ->
        when {
            uiState.isLoading -> WaterMeLoadingState("Loading upcoming care...", Modifier.padding(innerPadding))
            uiState.errorMessage != null -> Box(Modifier.padding(innerPadding).padding(20.dp)) {
                WaterMeErrorState(uiState.errorMessage, onRetryClick = { onEvent(CalendarEvent.RetryClicked) })
            }

            uiState.isEmpty -> Box(Modifier.padding(innerPadding).padding(20.dp)) {
                WaterMeEmptyState(
                    title = "Calendar is empty",
                    message = "New reminders will appear here automatically.",
                    icon = Icons.Rounded.Event,
                    actionLabel = "Jump to today",
                    onActionClick = { onEvent(CalendarEvent.TodayClicked) },
                )
            }

            else -> LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(GardenBackground),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    WaterMeCard {
                        WaterMeSectionHeader(
                            title = "Upcoming care",
                            actionLabel = "Today",
                            onActionClick = { onEvent(CalendarEvent.TodayClicked) },
                        )
                    }
                }
                uiState.days.forEach { day ->
                    item(key = day.dateLabel) {
                        WaterMeSectionHeader(day.dateLabel)
                    }
                    items(day.tasks, key = { it.id }) { task ->
                        CareTaskCard(
                            task = task,
                            onOpenPlant = { onEvent(CalendarEvent.TaskClicked(task.plantId, task.id)) },
                            onComplete = { onEvent(CalendarEvent.CompleteTask(task.id)) },
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarScreenPreview() {
    WaterMeTheme {
        CalendarScreen(
            uiState = CalendarUiState(days = WaterMePreviewData.calendarDays),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarEmptyPreview() {
    WaterMeTheme {
        CalendarScreen(uiState = CalendarUiState(), onEvent = {})
    }
}
