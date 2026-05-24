package com.hotelski.waterme.feature.today

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hotelski.waterme.appstate.WaterMeAppContainer
import com.hotelski.waterme.feature.common.endOfTodayMillis
import com.hotelski.waterme.feature.common.toCareTaskUiModel
import com.hotelski.waterme.feature.common.toHealthNoteUiModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface HomeEffect {
    data object NavigateToAddPlant : HomeEffect
    data class NavigateToPlantDetails(val plantId: String) : HomeEffect
}

private data class HomeActionState(
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

class HomeViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val plantRepository = WaterMeAppContainer.plantRepository(appContext)
    private val careRepository = WaterMeAppContainer.careRepository(appContext)

    private val actionState = MutableStateFlow(HomeActionState())
    private val _effects = MutableSharedFlow<HomeEffect>()

    val effects = _effects.asSharedFlow()

    val uiState = combine(
        careRepository.observeTodayTasks(endOfTodayMillis()),
        plantRepository.observePlantsWithDetails(WaterMeAppContainer.LOCAL_USER_ID),
        careRepository.observeRecentHealthNotes(WaterMeAppContainer.LOCAL_USER_ID),
        actionState,
    ) { tasks, plants, healthNotes, action ->
        TodayUiState(
            isLoading = false,
            tasks = tasks.map { it.toCareTaskUiModel() },
            healthNotes = healthNotes.map { it.toHealthNoteUiModel() },
            plantCount = plants.size,
            reminderCount = plants.sumOf { plant -> plant.reminders.count { it.isEnabled && it.deletedAt == null } },
            errorMessage = action.errorMessage,
            successMessage = action.successMessage,
        )
    }
        .catch { error -> emit(TodayUiState(errorMessage = error.toUserMessage())) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TodayUiState(isLoading = true),
        )

    init {
        seedDatabase()
    }

    fun onEvent(event: TodayEvent) {
        when (event) {
            TodayEvent.AddPlantClicked -> emitEffect(HomeEffect.NavigateToAddPlant)
            is TodayEvent.PlantClicked -> emitEffect(HomeEffect.NavigateToPlantDetails(event.plantId))
            is TodayEvent.CompleteTask -> completeTask(event.taskId)
            is TodayEvent.SkipTask -> skipTask(event.taskId)
            is TodayEvent.SnoozeTask -> snoozeTask(event.taskId)
            TodayEvent.RetryClicked -> seedDatabase()
        }
    }

    private fun completeTask(taskId: String) {
        viewModelScope.launch {
            runCatching { careRepository.markTaskCompleted(taskId) }
                .onSuccess { actionState.value = HomeActionState(successMessage = "Care task completed.") }
                .onFailure { actionState.value = HomeActionState(errorMessage = it.toUserMessage()) }
        }
    }

    private fun skipTask(taskId: String) {
        viewModelScope.launch {
            runCatching { careRepository.skipTask(taskId) }
                .onSuccess { actionState.value = HomeActionState(successMessage = "Care task skipped.") }
                .onFailure { actionState.value = HomeActionState(errorMessage = it.toUserMessage()) }
        }
    }

    private fun snoozeTask(taskId: String) {
        viewModelScope.launch {
            val snoozedUntil = System.currentTimeMillis() + SNOOZE_THREE_HOURS_MILLIS
            runCatching { careRepository.snoozeTask(taskId, snoozedUntil) }
                .onSuccess { actionState.value = HomeActionState(successMessage = "Reminder snoozed for 3 hours.") }
                .onFailure { actionState.value = HomeActionState(errorMessage = it.toUserMessage()) }
        }
    }

    private fun seedDatabase() {
        viewModelScope.launch {
            actionState.value = HomeActionState()
            runCatching { WaterMeAppContainer.seedIfEmpty(appContext) }
                .onFailure { actionState.value = HomeActionState(errorMessage = it.toUserMessage()) }
        }
    }

    private fun emitEffect(effect: HomeEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }

    private fun Throwable.toUserMessage(): String =
        message?.takeIf { it.isNotBlank() } ?: "WaterMe could not load today's care."

    private companion object {
        const val SNOOZE_THREE_HOURS_MILLIS = 10_800_000L
    }
}
