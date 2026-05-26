package com.hotelski.waterme.feature.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hotelski.waterme.appstate.WaterMeAppContainer
import com.hotelski.waterme.data.local.model.CareTaskWithPlant
import com.hotelski.waterme.feature.common.CareHistoryUiModel
import com.hotelski.waterme.feature.common.CareTaskUiModel
import com.hotelski.waterme.feature.common.toCareHistoryUiModel
import com.hotelski.waterme.feature.common.toCareTaskUiModel
import com.hotelski.waterme.feature.characters.activePlantCharacter
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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

private data class CalendarDataState(
    val tasks: List<CareTaskWithPlant>,
    val history: List<CareHistoryUiModel>,
    val plantOptions: List<CalendarPlantFilterUiModel>,
    val selectedDate: LocalDate,
    val selectedPlantId: String?,
)

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val plantRepository = WaterMeAppContainer.plantRepository(appContext)
    private val careRepository = WaterMeAppContainer.careRepository(appContext)
    private val settingsDataStore = WaterMeAppContainer.settingsDataStore(appContext)
    private val clock = Clock.systemDefaultZone()

    private val selectedDate = MutableStateFlow(LocalDate.now(clock))
    private val selectedPlantId = MutableStateFlow<String?>(null)
    private val actionState = MutableStateFlow(CalendarActionState())
    private val _effects = MutableSharedFlow<CalendarEffect>()

    val effects = _effects.asSharedFlow()

    private val monthTasks = selectedDate.flatMapLatest { date ->
        careRepository.observeCalendarTasks(
            startMillis = date.startOfMonthMillis(clock.zone),
            endMillis = date.startOfNextMonthMillis(clock.zone),
        )
    }

    private val monthHistory = selectedDate.flatMapLatest { date ->
        careRepository.observeFilteredCareHistoryForUser(
            userId = WaterMeAppContainer.LOCAL_USER_ID,
            plantId = null,
            careType = null,
            startMillis = date.startOfMonthMillis(clock.zone),
            endMillis = date.startOfNextMonthMillis(clock.zone) - 1,
        )
    }

    private val plantOptions = plantRepository
        .observePlantsWithDetails(WaterMeAppContainer.LOCAL_USER_ID)
        .map { plants ->
            plants
                .map { CalendarPlantFilterUiModel(id = it.plant.plantId, name = it.plant.name) }
                .sortedBy { it.name.lowercase() }
        }

    private val calendarData = combine(
        monthTasks,
        monthHistory,
        plantOptions,
        selectedDate,
        selectedPlantId,
    ) { tasks, history, plants, date, plantId ->
        CalendarDataState(
            tasks = tasks,
            history = history.map { it.toCareHistoryUiModel(clock) },
            plantOptions = plants,
            selectedDate = date,
            selectedPlantId = plantId,
        )
    }

    private val activeCharacter = combine(
        careRepository.observeCareHistoryForUser(WaterMeAppContainer.LOCAL_USER_ID),
        settingsDataStore.settings,
    ) { careHistory, settings ->
        activePlantCharacter(careHistory, settings.selectedCharacterId, clock)
    }

    val uiState = combine(calendarData, activeCharacter, actionState) { data, character, action ->
        val tasks = data.tasks
        val historyModels = data.history
        val plants = data.plantOptions
        val date = data.selectedDate
        val plantId = data.selectedPlantId
        val today = LocalDate.now(clock)
        val taskModels = tasks.map { it.toCareTaskUiModel(clock) }
        val taskDates = tasks.associate { it.taskId to it.effectiveDueAt.toLocalDate(clock.zone) }
        val selectedDayTasks = taskModels.filter { task -> taskDates[task.id] == date }
        val selectedDayLogs = historyModels.filter { it.performedAtMillis.toLocalDate(clock.zone) == date }
        val filteredMonthLogs = plantId?.let { selectedId ->
            historyModels.filter { it.plantId == selectedId }
        } ?: historyModels

        CalendarUiState(
            isLoading = false,
            dayStrip = buildDayStrip(
                selectedDate = date,
                tasks = tasks,
                history = historyModels,
            ),
            selectedDateMillis = date.toDatePickerMillis(),
            selectedDateLabel = date.toFriendlyDateLabel(today),
            selectedMonthLabel = date.format(monthFormatter),
            selectedDateIsFuture = date.isAfter(today),
            selectedDayTasks = selectedDayTasks,
            selectedDayLogs = selectedDayLogs,
            monthlyLogs = filteredMonthLogs,
            selectedPlantId = plantId,
            plantOptions = plants,
            activeCharacter = character,
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
            CalendarEvent.ScreenEntered -> {
                selectedDate.value = LocalDate.now(clock)
                actionState.value = CalendarActionState()
            }
            CalendarEvent.TodayClicked -> {
                selectedDate.value = LocalDate.now(clock)
                emitEffect(CalendarEffect.ScrollToToday)
            }
            is CalendarEvent.DateSelected -> {
                selectedDate.value = event.dateMillis.toDatePickerLocalDate()
                actionState.value = CalendarActionState()
            }
            is CalendarEvent.PlantFilterSelected -> selectedPlantId.value = event.plantId
            is CalendarEvent.TaskClicked -> emitEffect(CalendarEffect.NavigateToPlantDetails(event.plantId))
            is CalendarEvent.CompleteTask -> completeTask(event.taskId)
            CalendarEvent.RetryClicked -> seedDatabase()
        }
    }

    private fun completeTask(taskId: String) {
        if (selectedDate.value.isAfter(LocalDate.now(clock))) {
            actionState.value = CalendarActionState(errorMessage = "This task cannot be completed before its scheduled day.")
            return
        }

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

    private fun buildDayStrip(
        selectedDate: LocalDate,
        tasks: List<CareTaskWithPlant>,
        history: List<CareHistoryUiModel>,
    ): List<CalendarDateUiModel> {
        val today = LocalDate.now(clock)
        val month = YearMonth.from(selectedDate)
        val taskDates = tasks.map { it.effectiveDueAt.toLocalDate(clock.zone) }.toSet()
        val historyDates = history.map { it.performedAtMillis.toLocalDate(clock.zone) }.toSet()

        return (1..month.lengthOfMonth()).map { dayOfMonth ->
            val date = month.atDay(dayOfMonth)
            CalendarDateUiModel(
                dateMillis = date.toDatePickerMillis(),
                weekdayLabel = date.format(weekdayFormatter).uppercase(Locale.getDefault()),
                dayOfMonthLabel = dayOfMonth.toString(),
                accessibilityLabel = date.format(accessibilityDateFormatter),
                isToday = date == today,
                isSelected = date == selectedDate,
                hasCare = date in taskDates || date in historyDates,
            )
        }
    }

    private fun emitEffect(effect: CalendarEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }

    private fun Throwable.toUserMessage(): String =
        message?.takeIf { it.isNotBlank() } ?: "WaterMe could not load the calendar."
}

private fun LocalDate.startOfMonthMillis(zoneId: ZoneId): Long =
    withDayOfMonth(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

private fun LocalDate.startOfNextMonthMillis(zoneId: ZoneId): Long =
    plusMonths(1).withDayOfMonth(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

private fun Long.toLocalDate(zoneId: ZoneId): LocalDate =
    Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()

private fun Long.toDatePickerLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

private fun LocalDate.toDatePickerMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun LocalDate.toFriendlyDateLabel(today: LocalDate): String =
    when (this) {
        today -> "Today, ${format(shortDateFormatter)}"
        today.plusDays(1) -> "Tomorrow, ${format(shortDateFormatter)}"
        today.minusDays(1) -> "Yesterday, ${format(shortDateFormatter)}"
        else -> format(fullDateFormatter)
    }

private val weekdayFormatter = DateTimeFormatter.ofPattern("EEE")
private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
private val shortDateFormatter = DateTimeFormatter.ofPattern("MMM d")
private val fullDateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
private val accessibilityDateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")
