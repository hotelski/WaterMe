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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val editingNoteId: String? = null,
)

private data class PlantDetailsActionState(
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val heartBurstKey: Long = 0L,
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
    private val reminderNotifications = WaterMeAppContainer.reminderNotificationCoordinator(appContext)

    private val plantId: String = checkNotNull(savedStateHandle[WaterMeRoute.PlantDetails.PLANT_ID_ARG])
    private val healthDraft = MutableStateFlow(HealthNoteDraftState())
    private val actionState = MutableStateFlow(PlantDetailsActionState())
    private val _effects = MutableSharedFlow<PlantDetailsEffect>()
    private var messageDismissJob: Job? = null

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
        plantRepository.observePlants(WaterMeAppContainer.LOCAL_USER_ID),
        settingsDataStore.settings,
    ) { careHistory, plants, settings ->
        activePlantCharacter(
            careHistory = careHistory,
            selectedCharacterId = settings.selectedCharacterId,
            plantsAddedTotal = plants.size,
            appOpenDayStreak = settings.appOpenDayStreak,
        )
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
            editingHealthNoteId = draft.editingNoteId,
            activeCharacter = character,
            isDeleting = action.isDeleting,
            showDeleteConfirmation = action.showDeleteConfirmation,
            errorMessage = action.errorMessage,
            successMessage = action.successMessage,
            heartBurstKey = action.heartBurstKey,
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
            PlantDetailsEvent.CancelHealthNoteEditClicked -> {
                healthDraft.value = HealthNoteDraftState(mood = healthDraft.value.mood)
                actionState.value = PlantDetailsActionState()
            }
            is PlantDetailsEvent.CompleteTask -> completeTask(event.taskId)
            is PlantDetailsEvent.SkipTask -> skipTask(event.taskId)
            is PlantDetailsEvent.SnoozeTask -> snoozeTask(event.taskId)
            is PlantDetailsEvent.EditHealthNoteClicked -> {
                healthDraft.value = HealthNoteDraftState(
                    note = event.note,
                    mood = event.mood,
                    editingNoteId = event.noteId,
                )
                actionState.value = PlantDetailsActionState()
            }
            is PlantDetailsEvent.DeleteHealthNoteClicked -> deleteHealthNote(event.noteId)
            is PlantDetailsEvent.HealthNoteChanged -> updateHealthNote(event.value)
            is PlantDetailsEvent.HealthMoodSelected -> healthDraft.update { it.copy(mood = event.mood) }
            PlantDetailsEvent.RetryClicked -> actionState.value = PlantDetailsActionState()
        }
    }

    private fun deletePlant() {
        viewModelScope.launch {
            actionState.value = PlantDetailsActionState(isDeleting = true, showDeleteConfirmation = false)
            runCatching {
                plantRepository.deletePlant(plantId)
                reminderNotifications.syncScheduledReminders()
            }
                .onSuccess {
                    showMessage(successMessage = "Plant deleted.")
                    _effects.emit(PlantDetailsEffect.NavigateToPlantsAfterDelete)
                }
                .onFailure { showMessage(errorMessage = it.toUserMessage()) }
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
                .onFailure { showMessage(errorMessage = it.toUserMessage()) }
        }
    }

    private fun skipTask(taskId: String) {
        viewModelScope.launch {
            runCatching {
                careRepository.skipTask(taskId)
                reminderNotifications.syncScheduledReminders()
            }
                .onSuccess { showMessage(successMessage = "Care task skipped.") }
                .onFailure { showMessage(errorMessage = it.toUserMessage()) }
        }
    }

    private fun snoozeTask(taskId: String) {
        viewModelScope.launch {
            val snoozedUntil = System.currentTimeMillis() + SNOOZE_THREE_HOURS_MILLIS
            runCatching {
                careRepository.snoozeTask(taskId, snoozedUntil)
                reminderNotifications.syncScheduledReminders()
            }
                .onSuccess { showMessage(successMessage = "Reminder snoozed for 3 hours.") }
                .onFailure { showMessage(errorMessage = it.toUserMessage()) }
        }
    }

    private fun updateHealthNote(value: String) {
        healthDraft.update { it.copy(note = value.take(MAX_HEALTH_NOTE_LENGTH)) }
        if (value.length > MAX_HEALTH_NOTE_LENGTH) {
            showMessage(errorMessage = "Health notes are limited to $MAX_HEALTH_NOTE_LENGTH characters.")
        } else {
            actionState.value = PlantDetailsActionState()
        }
    }

    private fun addHealthNote() {
        val draft = healthDraft.value
        if (draft.note.isBlank()) {
            showMessage(errorMessage = "Add a note before saving.")
            return
        }

        viewModelScope.launch {
            runCatching {
                val editingNoteId = draft.editingNoteId
                if (editingNoteId == null) {
                    careRepository.logHealthNote(
                        plantId = plantId,
                        mood = draft.mood,
                        notes = draft.note,
                    )
                } else {
                    careRepository.updateHealthNote(
                        historyId = editingNoteId,
                        mood = draft.mood,
                        notes = draft.note,
                    )
                }
            }
                .onSuccess {
                    healthDraft.value = HealthNoteDraftState(mood = draft.mood)
                    showMessage(
                        successMessage = if (draft.editingNoteId == null) "Health note added." else "Health note updated.",
                    )
                }
                .onFailure { showMessage(errorMessage = it.toUserMessage()) }
        }
    }

    private fun deleteHealthNote(noteId: String) {
        viewModelScope.launch {
            runCatching { careRepository.deleteCareHistoryEntry(noteId) }
                .onSuccess {
                    if (healthDraft.value.editingNoteId == noteId) {
                        healthDraft.value = HealthNoteDraftState(mood = healthDraft.value.mood)
                    }
                    showMessage(successMessage = "Health note deleted.")
                }
                .onFailure { showMessage(errorMessage = it.toUserMessage()) }
        }
    }

    private fun showMessage(
        successMessage: String? = null,
        errorMessage: String? = null,
        heartBurstKey: Long = 0L,
    ) {
        actionState.value = PlantDetailsActionState(
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

    private fun emitEffect(effect: PlantDetailsEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }

    private fun Throwable.toUserMessage(): String =
        message?.takeIf { it.isNotBlank() } ?: "WaterMe could not load this plant."

    private companion object {
        const val SNOOZE_THREE_HOURS_MILLIS = 10_800_000L
        const val MAX_HEALTH_NOTE_LENGTH = 280
        const val MESSAGE_VISIBLE_MILLIS = 2_400L
    }
}
