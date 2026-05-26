package com.hotelski.waterme.feature.today

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hotelski.waterme.appstate.WaterMeAppContainer
import com.hotelski.waterme.data.local.entity.HistoryAction
import com.hotelski.waterme.feature.common.endOfTodayMillis
import com.hotelski.waterme.feature.common.startOfTodayMillis
import com.hotelski.waterme.feature.common.toCareTaskUiModel
import com.hotelski.waterme.feature.common.toHealthNoteUiModel
import com.hotelski.waterme.feature.common.toPlantCardUiModel
import com.hotelski.waterme.feature.common.toReminderUiModel
import com.hotelski.waterme.feature.characters.activePlantCharacter
import com.hotelski.waterme.model.HealthMood
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
    data object NavigateToCalendar : HomeEffect
    data object NavigateToPlants : HomeEffect
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
    private val settingsDataStore = WaterMeAppContainer.settingsDataStore(appContext)

    private val actionState = MutableStateFlow(HomeActionState())
    private val _effects = MutableSharedFlow<HomeEffect>()

    val effects = _effects.asSharedFlow()

    val uiState = combine(
        careRepository.observeTasksDueBy(endOfTodayMillis()),
        plantRepository.observePlantsWithDetails(WaterMeAppContainer.LOCAL_USER_ID),
        careRepository.observeCareHistoryForUser(WaterMeAppContainer.LOCAL_USER_ID),
        settingsDataStore.settings,
        actionState,
    ) { tasks, plants, careHistory, settings, action ->
        val todayStartMillis = startOfTodayMillis()
        val todayTasks = tasks.filter { it.effectiveDueAt >= todayStartMillis }
        val overdueTasks = tasks.filter { it.effectiveDueAt < todayStartMillis }
        val activeReminders = plants.flatMap { plant ->
            plant.reminders.filter { it.isEnabled && it.deletedAt == null }
        }
        val completedTodayCount = careHistory.count {
            it.action == HistoryAction.COMPLETED && it.performedAt >= todayStartMillis
        }
        val dueTaskCount = tasks.size
        val completedPercent = if (dueTaskCount == 0) {
            1f
        } else {
            completedTodayCount.toFloat() / (completedTodayCount + dueTaskCount).coerceAtLeast(1)
        }
        val healthNotes = careHistory
            .filter { it.action == HistoryAction.HEALTH_NOTE }
            .take(5)

        TodayUiState(
            isLoading = false,
            tasks = todayTasks.map { it.toCareTaskUiModel() },
            overdueTasks = overdueTasks.map { it.toCareTaskUiModel() },
            upcomingReminders = activeReminders
                .filter { it.nextDueAt > endOfTodayMillis() }
                .sortedBy { it.nextDueAt }
                .take(5)
                .map { it.toReminderUiModel() },
            healthNotes = healthNotes.map { it.toHealthNoteUiModel() },
            healthSummary = PlantHealthSummaryUiModel(
                attentionCount = healthNotes.count { it.healthMood == HealthMood.ATTENTION },
                healthyCount = healthNotes.count { it.healthMood == HealthMood.HEALTHY },
                newGrowthCount = healthNotes.count { it.healthMood == HealthMood.GROWTH },
            ),
            recentlyAddedPlants = plants
                .sortedByDescending { it.plant.createdAt }
                .take(4)
                .map { it.toPlantCardUiModel() },
            progressStats = DashboardProgressUiModel(
                completedToday = completedTodayCount,
                dueToday = dueTaskCount,
                overdue = overdueTasks.size,
                completionPercent = completedPercent.coerceIn(0f, 1f),
            ),
            plantCount = plants.size,
            reminderCount = activeReminders.size,
            activeCharacter = activePlantCharacter(careHistory, settings.selectedCharacterId),
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
            TodayEvent.CalendarClicked -> emitEffect(HomeEffect.NavigateToCalendar)
            TodayEvent.MyPlantsClicked -> emitEffect(HomeEffect.NavigateToPlants)
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
