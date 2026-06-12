package com.hotelski.waterme.feature.today

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hotelski.waterme.appstate.WaterMeAppContainer
import com.hotelski.waterme.data.local.entity.HistoryAction
import com.hotelski.waterme.feature.characters.activePlantCharacter
import com.hotelski.waterme.feature.common.endOfTodayMillis
import com.hotelski.waterme.feature.common.startOfTodayMillis
import com.hotelski.waterme.feature.common.toCareTaskUiModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

sealed interface HomeEffect {
    data object NavigateToAddPlant : HomeEffect
    data object NavigateToCalendar : HomeEffect
    data object NavigateToDonate : HomeEffect
    data object NavigateToFeedback : HomeEffect
    data object NavigateToGuide : HomeEffect
    data object NavigateToPlants : HomeEffect
}

private data class HomeActionState(
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val heartBurstKey: Long = 0L,
    val isRefreshing: Boolean = false,
)

class HomeViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val plantRepository = WaterMeAppContainer.plantRepository(appContext)
    private val careRepository = WaterMeAppContainer.careRepository(appContext)
    private val settingsDataStore = WaterMeAppContainer.settingsDataStore(appContext)
    private val reminderNotifications = WaterMeAppContainer.reminderNotificationCoordinator(appContext)

    private val actionState = MutableStateFlow(HomeActionState())
    private val _effects = MutableSharedFlow<HomeEffect>()
    private var messageDismissJob: Job? = null

    val effects = _effects.asSharedFlow()

    val uiState = combine(
        careRepository.observeTasksDueBy(endOfTodayMillis()),
        plantRepository.observePlantsWithDetails(WaterMeAppContainer.LOCAL_USER_ID),
        careRepository.observeCareHistoryForUser(WaterMeAppContainer.LOCAL_USER_ID),
        settingsDataStore.settings,
        actionState,
    ) { tasks, plants, careHistory, settings, action ->
        val todayStartMillis = startOfTodayMillis()
        val weekStartMillis = startOfWeekMillis()

        val todayTasks = tasks.filter { it.effectiveDueAt >= todayStartMillis }
        val overdueTasks = tasks.filter { it.effectiveDueAt < todayStartMillis }

        val activeReminderCount = plants.sumOf { plant ->
            plant.reminders.count { reminder ->
                reminder.isEnabled && reminder.deletedAt == null
            }
        }

        val completedTodayCount = careHistory.count {
            it.action == HistoryAction.COMPLETED && it.performedAt >= todayStartMillis
        }

        val completedThisWeekCount = careHistory.count {
            it.action == HistoryAction.COMPLETED && it.performedAt >= weekStartMillis
        }

        val careHistoryCount = careHistory.count { it.action != HistoryAction.HEALTH_NOTE }
        val noteCount = plants.count { it.plant.notes.isNotBlank() } +
            careHistory.count { !it.notes.isNullOrBlank() }

        val dueTaskCount = tasks.size

        val completedPercent = if (dueTaskCount == 0) {
            1f
        } else {
            completedTodayCount.toFloat() / (completedTodayCount + dueTaskCount).coerceAtLeast(1)
        }

        TodayUiState(
            isLoading = false,
            tasks = todayTasks.map { it.toCareTaskUiModel() },
            overdueTasks = overdueTasks.map { it.toCareTaskUiModel() },
            progressStats = DashboardProgressUiModel(
                completedToday = completedTodayCount,
                dueToday = dueTaskCount,
                overdue = overdueTasks.size,
                completionPercent = completedPercent.coerceIn(0f, 1f),
            ),
            plantCount = plants.size,
            reminderCount = activeReminderCount,
            careHistoryCount = careHistoryCount,
            noteCount = noteCount,
            appOpenDayStreak = settings.appOpenDayStreak,
            completedThisWeek = completedThisWeekCount,
            activeCharacter = activePlantCharacter(
                careHistory = careHistory,
                selectedCharacterId = settings.selectedCharacterId,
                plantsAddedTotal = plants.size,
                appOpenDayStreak = settings.appOpenDayStreak,
            ),
            isRefreshing = action.isRefreshing,
            errorMessage = action.errorMessage,
            successMessage = action.successMessage,
            heartBurstKey = action.heartBurstKey,
        )
    }
        .catch { error ->
            emit(TodayUiState(errorMessage = error.toUserMessage()))
        }
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
            TodayEvent.DonateClicked -> emitEffect(HomeEffect.NavigateToDonate)
            TodayEvent.FeedbackClicked -> emitEffect(HomeEffect.NavigateToFeedback)
            TodayEvent.HowToUseClicked -> emitEffect(HomeEffect.NavigateToGuide)
            TodayEvent.MyPlantsClicked -> emitEffect(HomeEffect.NavigateToPlants)
            TodayEvent.RetryClicked -> seedDatabase()
            TodayEvent.RefreshPulled -> refreshHome()
            is TodayEvent.CompleteTask -> completeTask(event.taskId)
            is TodayEvent.SkipTask -> skipTask(event.taskId)
            is TodayEvent.SnoozeTask -> snoozeTask(event.taskId)
        }
    }

    private fun completeTask(taskId: String) {
        viewModelScope.launch {
            runCatching {
                careRepository.markTaskCompleted(taskId)
                reminderNotifications.syncScheduledReminders()
            }
                .onSuccess {
                    showMessage(
                        successMessage = "Care task completed.",
                        heartBurstKey = System.nanoTime(),
                    )
                }
                .onFailure {
                    showMessage(
                        errorMessage = it.toUserMessage(),
                    )
                }
        }
    }

    private fun skipTask(taskId: String) {
        viewModelScope.launch {
            runCatching {
                careRepository.skipTask(taskId)
                reminderNotifications.syncScheduledReminders()
            }
                .onSuccess {
                    showMessage(
                        successMessage = "Care task skipped.",
                    )
                }
                .onFailure {
                    showMessage(
                        errorMessage = it.toUserMessage(),
                    )
                }
        }
    }

    private fun snoozeTask(taskId: String) {
        viewModelScope.launch {
            val snoozedUntil = System.currentTimeMillis() + SNOOZE_THREE_HOURS_MILLIS

            runCatching {
                careRepository.snoozeTask(taskId, snoozedUntil)
                reminderNotifications.syncScheduledReminders()
            }
                .onSuccess {
                    showMessage(
                        successMessage = "Reminder snoozed for 3 hours.",
                    )
                }
                .onFailure {
                    showMessage(
                        errorMessage = it.toUserMessage(),
                    )
                }
        }
    }

    private fun seedDatabase() {
        viewModelScope.launch {
            actionState.value = HomeActionState()

            runCatching {
                WaterMeAppContainer.seedIfEmpty(appContext)
            }
                .onFailure {
                    showMessage(
                        errorMessage = it.toUserMessage(),
                    )
                }
        }
    }

    private fun refreshHome() {
        if (actionState.value.isRefreshing) return

        viewModelScope.launch {
            actionState.value = HomeActionState(isRefreshing = true)
            val result = runCatching {
                WaterMeAppContainer.seedIfEmpty(appContext)
                reminderNotifications.syncScheduledReminders()
            }
            delay(LEAF_REFRESH_VISIBLE_MILLIS)
            actionState.value = actionState.value.copy(isRefreshing = false)
            result.onFailure {
                showMessage(
                    errorMessage = it.toUserMessage(),
                )
            }
        }
    }

    private fun showMessage(
        successMessage: String? = null,
        errorMessage: String? = null,
        heartBurstKey: Long = 0L,
    ) {
        actionState.value = HomeActionState(
            successMessage = successMessage,
            errorMessage = errorMessage,
            heartBurstKey = heartBurstKey,
        )
        messageDismissJob?.cancel()
        messageDismissJob = viewModelScope.launch {
            delay(MESSAGE_VISIBLE_MILLIS)
            val current = actionState.value
            if (
                current.successMessage == successMessage &&
                current.errorMessage == errorMessage &&
                current.heartBurstKey == heartBurstKey
            ) {
                actionState.value = current.copy(successMessage = null, errorMessage = null, heartBurstKey = 0L)
            }
        }
    }

    private fun emitEffect(effect: HomeEffect) {
        viewModelScope.launch {
            _effects.emit(effect)
        }
    }

    private fun startOfWeekMillis(): Long {
        val calendar = Calendar.getInstance()

        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return calendar.timeInMillis
    }

    private fun Throwable.toUserMessage(): String =
        message?.takeIf { it.isNotBlank() } ?: "WaterMe could not load today's care."

    private companion object {
        const val SNOOZE_THREE_HOURS_MILLIS = 10_800_000L
        const val MESSAGE_VISIBLE_MILLIS = 2_400L
        const val LEAF_REFRESH_VISIBLE_MILLIS = 650L
    }
}
