package com.hotelski.waterme.feature.plantdetails

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.hotelski.waterme.appstate.WaterMeAppContainer
import com.hotelski.waterme.data.local.entity.CareHistoryEntity
import com.hotelski.waterme.data.local.entity.CareTaskEntity
import com.hotelski.waterme.data.local.entity.HistoryAction
import com.hotelski.waterme.data.local.entity.ReminderEntity
import com.hotelski.waterme.data.local.entity.TaskStatus
import com.hotelski.waterme.data.local.model.PlantWithDetails
import com.hotelski.waterme.feature.common.toCareHistoryUiModel
import com.hotelski.waterme.feature.common.toCareTaskUiModel
import com.hotelski.waterme.feature.common.toHealthNoteUiModel
import com.hotelski.waterme.feature.common.toPlantDetailsUiModel
import com.hotelski.waterme.feature.common.toReminderUiModel
import com.hotelski.waterme.feature.characters.activePlantCharacter
import com.hotelski.waterme.model.HealthMood
import com.hotelski.waterme.navigation.WaterMeRoute
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface PlantDetailsEffect {
    data object NavigateBack : PlantDetailsEffect
    data object NavigateToPlantsAfterDelete : PlantDetailsEffect
    data class NavigateToEditPlant(val plantId: String) : PlantDetailsEffect
    data class NavigateToCareHistory(val plantId: String) : PlantDetailsEffect
}

private data class HealthNoteDraftState(
    val note: String = "",
    val mood: HealthMood = HealthMood.ATTENTION,
)

private data class PlantDetailsActionState(
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isDeleting: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
)

private data class PlantDetailsData(
    val plantDetails: PlantWithDetails?,
    val reminders: List<ReminderEntity>,
    val tasks: List<CareTaskEntity>,
    val history: List<CareHistoryEntity>,
)

class PlantDetailsViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val plantRepository = WaterMeAppContainer.plantRepository(appContext)
    private val reminderRepository = WaterMeAppContainer.reminderRepository(appContext)
    private val careRepository = WaterMeAppContainer.careRepository(appContext)
    private val settingsDataStore = WaterMeAppContainer.settingsDataStore(appContext)

    private val plantId: String = checkNotNull(savedStateHandle[WaterMeRoute.PlantDetails.PLANT_ID_ARG])
    private val healthDraft = MutableStateFlow(HealthNoteDraftState())
    private val actionState = MutableStateFlow(PlantDetailsActionState())
    private val _effects = MutableSharedFlow<PlantDetailsEffect>()

    val effects = _effects.asSharedFlow()

    private val plantDetailsData = combine(
        plantRepository.observePlantWithDetails(plantId),
        reminderRepository.observeRemindersForPlant(plantId),
        careRepository.observeTasksForPlant(plantId),
        careRepository.observeCareHistoryForPlant(plantId),
    ) { plantDetails, reminders, tasks, history ->
        PlantDetailsData(
            plantDetails = plantDetails,
            reminders = reminders,
            tasks = tasks,
            history = history,
        )
    }

    private val activeCharacter = combine(
        careRepository.observeCareHistoryForUser(WaterMeAppContainer.LOCAL_USER_ID),
        settingsDataStore.settings,
    ) { careHistory, settings ->
        activePlantCharacter(careHistory, settings.selectedCharacterId)
    }

    val uiState = combine(
        plantDetailsData,
        activeCharacter,
        healthDraft,
        actionState,
    ) { data, character, draft, action ->
        val plantUi = data.plantDetails?.toPlantDetailsUiModel()
        PlantDetailsUiState(
            isLoading = false,
            plant = plantUi,
            reminders = data.reminders
                .filter { it.deletedAt == null }
                .sortedBy { it.nextDueAt }
                .map { it.toReminderUiModel() },
            pendingTasks = if (plantUi == null) {
                emptyList()
            } else {
                data.tasks
                    .filter { it.status == TaskStatus.PENDING || it.status == TaskStatus.SNOOZED }
                    .sortedBy { it.effectiveDueAt }
                    .map { it.toCareTaskUiModel(plantUi) }
            },
            careHistory = data.history
                .filter { it.action != HistoryAction.HEALTH_NOTE }
                .map { it.toCareHistoryUiModel(plantUi?.name.orEmpty()) },
            healthNotes = data.history
                .filter { it.action == HistoryAction.HEALTH_NOTE }
                .map { it.toHealthNoteUiModel(plantUi?.name.orEmpty()) },
            healthNoteDraft = draft.note,
            selectedHealthMood = draft.mood,
            activeCharacter = character,
            isDeleting = action.isDeleting,
            showDeleteConfirmation = action.showDeleteConfirmation,
            errorMessage = action.errorMessage,
            successMessage = action.successMessage,
        )
    }
        .catch { error -> emit(PlantDetailsUiState(errorMessage = error.toUserMessage())) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlantDetailsUiState(isLoading = true),
        )

    fun onEvent(event: PlantDetailsEvent) {
        when (event) {
            PlantDetailsEvent.BackClicked -> emitEffect(PlantDetailsEffect.NavigateBack)
            PlantDetailsEvent.EditClicked -> emitEffect(PlantDetailsEffect.NavigateToEditPlant(plantId))
            PlantDetailsEvent.ViewAllHistoryClicked -> emitEffect(PlantDetailsEffect.NavigateToCareHistory(plantId))
            PlantDetailsEvent.DeleteClicked -> actionState.update { it.copy(showDeleteConfirmation = true) }
            PlantDetailsEvent.DismissDeleteClicked -> actionState.update { it.copy(showDeleteConfirmation = false) }
            PlantDetailsEvent.ConfirmDeleteClicked -> deletePlant()
            PlantDetailsEvent.AddHealthNoteClicked -> addHealthNote()
            is PlantDetailsEvent.CompleteTask -> completeTask(event.taskId)
            is PlantDetailsEvent.SkipTask -> skipTask(event.taskId)
            is PlantDetailsEvent.SnoozeTask -> snoozeTask(event.taskId)
            is PlantDetailsEvent.HealthNoteChanged -> updateHealthNote(event.value)
            is PlantDetailsEvent.HealthMoodSelected -> healthDraft.update { it.copy(mood = event.mood) }
            PlantDetailsEvent.RetryClicked -> actionState.value = PlantDetailsActionState()
        }
    }

    private fun deletePlant() {
        viewModelScope.launch {
            actionState.value = PlantDetailsActionState(isDeleting = true, showDeleteConfirmation = false)
            runCatching { plantRepository.deletePlant(plantId) }
                .onSuccess {
                    actionState.value = PlantDetailsActionState(successMessage = "Plant deleted.")
                    _effects.emit(PlantDetailsEffect.NavigateToPlantsAfterDelete)
                }
                .onFailure { actionState.value = PlantDetailsActionState(errorMessage = it.toUserMessage()) }
        }
    }

    private fun completeTask(taskId: String) {
        viewModelScope.launch {
            runCatching { careRepository.markTaskCompleted(taskId) }
                .onSuccess { actionState.value = PlantDetailsActionState(successMessage = "Care task completed.") }
                .onFailure { actionState.value = PlantDetailsActionState(errorMessage = it.toUserMessage()) }
        }
    }

    private fun skipTask(taskId: String) {
        viewModelScope.launch {
            runCatching { careRepository.skipTask(taskId) }
                .onSuccess { actionState.value = PlantDetailsActionState(successMessage = "Care task skipped.") }
                .onFailure { actionState.value = PlantDetailsActionState(errorMessage = it.toUserMessage()) }
        }
    }

    private fun snoozeTask(taskId: String) {
        viewModelScope.launch {
            val snoozedUntil = System.currentTimeMillis() + SNOOZE_THREE_HOURS_MILLIS
            runCatching { careRepository.snoozeTask(taskId, snoozedUntil) }
                .onSuccess { actionState.value = PlantDetailsActionState(successMessage = "Reminder snoozed for 3 hours.") }
                .onFailure { actionState.value = PlantDetailsActionState(errorMessage = it.toUserMessage()) }
        }
    }

    private fun updateHealthNote(value: String) {
        healthDraft.update { it.copy(note = value.take(MAX_HEALTH_NOTE_LENGTH)) }
        actionState.value = if (value.length > MAX_HEALTH_NOTE_LENGTH) {
            PlantDetailsActionState(errorMessage = "Health notes are limited to $MAX_HEALTH_NOTE_LENGTH characters.")
        } else {
            PlantDetailsActionState()
        }
    }

    private fun addHealthNote() {
        val draft = healthDraft.value
        if (draft.note.isBlank()) {
            actionState.value = PlantDetailsActionState(errorMessage = "Add a note before saving.")
            return
        }

        viewModelScope.launch {
            runCatching {
                careRepository.logHealthNote(
                    plantId = plantId,
                    mood = draft.mood,
                    notes = draft.note,
                )
            }
                .onSuccess {
                    healthDraft.value = HealthNoteDraftState(mood = draft.mood)
                    actionState.value = PlantDetailsActionState(successMessage = "Health note added.")
                }
                .onFailure { actionState.value = PlantDetailsActionState(errorMessage = it.toUserMessage()) }
        }
    }

    private fun emitEffect(effect: PlantDetailsEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }

    private fun Throwable.toUserMessage(): String =
        message?.takeIf { it.isNotBlank() } ?: "WaterMe could not load this plant."

    private companion object {
        const val SNOOZE_THREE_HOURS_MILLIS = 10_800_000L
        const val MAX_HEALTH_NOTE_LENGTH = 280
    }
}
