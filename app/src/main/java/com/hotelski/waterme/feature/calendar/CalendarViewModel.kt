package com.hotelski.waterme.feature.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hotelski.waterme.appstate.WaterMeAppContainer
import com.hotelski.waterme.feature.common.calendarDaysFromTasks
import com.hotelski.waterme.feature.common.calendarWindowEndMillis
import com.hotelski.waterme.feature.common.startOfTodayMillis
import com.hotelski.waterme.feature.common.toCareTaskUiModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CalendarEffect {
    data object ScrollToToday : CalendarEffect
    data class NavigateToPlantDetails(val plantId: String) : CalendarEffect
}

private data class CalendarActionState(
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

class CalendarViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val careRepository = WaterMeAppContainer.careRepository(appContext)

    private val actionState = MutableStateFlow(CalendarActionState())
    private val _effects = MutableSharedFlow<CalendarEffect>()

    val effects = _effects.asSharedFlow()

    private val calendarTasks = careRepository.observeCalendarTasks(
        startMillis = startOfTodayMillis(),
        endMillis = calendarWindowEndMillis(DEFAULT_CALENDAR_DAYS_AHEAD),
    )

    val uiState = combine(calendarTasks, actionState) { tasks, action ->
        val taskDates = tasks.associate { it.taskId to it.effectiveDueAt }
        val taskModels = tasks.map { it.toCareTaskUiModel() }
        CalendarUiState(
            isLoading = false,
            days = calendarDaysFromTasks(taskModels, taskDates),
            errorMessage = action.errorMessage,
            successMessage = action.successMessage,
        )
    }
        .catch { error -> emit(CalendarUiState(errorMessage = error.toUserMessage())) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CalendarUiState(isLoading = true),
        )

    init {
        seedDatabase()
    }

    fun onEvent(event: CalendarEvent) {
        when (event) {
            CalendarEvent.TodayClicked -> {
                emitEffect(CalendarEffect.ScrollToToday)
            }
            is CalendarEvent.TaskClicked -> emitEffect(CalendarEffect.NavigateToPlantDetails(event.plantId))
            is CalendarEvent.CompleteTask -> completeTask(event.taskId)
            CalendarEvent.RetryClicked -> seedDatabase()
        }
    }

    private fun completeTask(taskId: String) {
        viewModelScope.launch {
            runCatching { careRepository.markCalendarTaskCompleted(taskId) }
                .onSuccess { actionState.value = CalendarActionState(successMessage = "Care task completed.") }
                .onFailure { actionState.value = CalendarActionState(errorMessage = it.toUserMessage()) }
        }
    }

    private fun seedDatabase() {
        viewModelScope.launch {
            actionState.value = CalendarActionState()
            runCatching { WaterMeAppContainer.seedIfEmpty(appContext) }
                .onFailure { actionState.value = CalendarActionState(errorMessage = it.toUserMessage()) }
        }
    }

    private fun emitEffect(effect: CalendarEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }

    private fun Throwable.toUserMessage(): String =
        message?.takeIf { it.isNotBlank() } ?: "WaterMe could not load the calendar."
}

private const val DEFAULT_CALENDAR_DAYS_AHEAD = 30L
