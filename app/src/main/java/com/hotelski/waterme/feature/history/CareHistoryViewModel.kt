package com.hotelski.waterme.feature.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.hotelski.waterme.appstate.WaterMeAppContainer
import com.hotelski.waterme.feature.common.CareHistoryUiModel
import com.hotelski.waterme.feature.common.toCareHistoryUiModel
import com.hotelski.waterme.model.CareType
import com.hotelski.waterme.navigation.WaterMeRoute
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlantFilterUiModel(
    val id: String,
    val name: String,
)

enum class CareHistoryDateRange(
    val label: String,
    private val daysBack: Long?,
) {
    ALL_TIME("All time", null),
    TODAY("Today", 0),
    LAST_7_DAYS("7 days", 7),
    LAST_30_DAYS("30 days", 30),
    LAST_90_DAYS("90 days", 90);

    fun bounds(clock: Clock = Clock.systemDefaultZone()): Pair<Long?, Long?> {
        val endMillis = Instant.now(clock).toEpochMilli()
        val startMillis = when (daysBack) {
            null -> null
            0L -> LocalDate.now(clock).atStartOfDay(clock.zone).toInstant().toEpochMilli()
            else -> LocalDate.now(clock).minusDays(daysBack).atStartOfDay(clock.zone).toInstant().toEpochMilli()
        }
        return startMillis to if (daysBack == null) null else endMillis
    }
}

enum class CareHistoryPerformedAtOption(
    val label: String,
    private val daysAgo: Long,
) {
    TODAY("Today", 0),
    YESTERDAY("Yesterday", 1),
    LAST_WEEK("Last week", 7);

    fun toMillis(clock: Clock = Clock.systemDefaultZone()): Long =
        Instant.now(clock).minus(daysAgo, ChronoUnit.DAYS).toEpochMilli()

    companion object {
        fun fromMillis(value: Long, clock: Clock = Clock.systemDefaultZone()): CareHistoryPerformedAtOption {
            val date = Instant.ofEpochMilli(value).atZone(clock.zone).toLocalDate()
            val today = LocalDate.now(clock)
            return when (date) {
                today -> TODAY
                today.minusDays(1) -> YESTERDAY
                else -> LAST_WEEK
            }
        }
    }
}

data class CareHistoryDraftUiState(
    val isVisible: Boolean = false,
    val editingEntryId: String? = null,
    val plantId: String = "",
    val careType: CareType = CareType.WATERING,
    val performedAtOption: CareHistoryPerformedAtOption = CareHistoryPerformedAtOption.TODAY,
    val notes: String = "",
    val photoUri: String = "",
    val errorMessage: String? = null,
) {
    val isEditing: Boolean
        get() = editingEntryId != null
}

data class CareHistoryUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val selectedPlantId: String? = null,
    val selectedCareType: CareType? = null,
    val selectedDateRange: CareHistoryDateRange = CareHistoryDateRange.ALL_TIME,
    val plantOptions: List<PlantFilterUiModel> = emptyList(),
    val entries: List<CareHistoryUiModel> = emptyList(),
    val draft: CareHistoryDraftUiState = CareHistoryDraftUiState(),
    val pendingDeleteEntryId: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && entries.isEmpty()

    val hasActiveFilters: Boolean
        get() = selectedPlantId != null ||
            selectedCareType != null ||
            selectedDateRange != CareHistoryDateRange.ALL_TIME

    val selectedPlantLabel: String
        get() = plantOptions.firstOrNull { it.id == selectedPlantId }?.name ?: "All plants"
}

sealed interface CareHistoryEvent {
    data object BackClicked : CareHistoryEvent
    data object RetryClicked : CareHistoryEvent
    data object AddManualEntryClicked : CareHistoryEvent
    data object DismissDraftClicked : CareHistoryEvent
    data object SaveDraftClicked : CareHistoryEvent
    data object DraftPhotoPickerClicked : CareHistoryEvent
    data object ClearFiltersClicked : CareHistoryEvent
    data object DismissDeleteClicked : CareHistoryEvent
    data object ConfirmDeleteClicked : CareHistoryEvent
    data class PlantFilterSelected(val plantId: String?) : CareHistoryEvent
    data class CareTypeFilterSelected(val careType: CareType?) : CareHistoryEvent
    data class DateRangeSelected(val dateRange: CareHistoryDateRange) : CareHistoryEvent
    data class EditEntryClicked(val entry: CareHistoryUiModel) : CareHistoryEvent
    data class DeleteEntryClicked(val historyId: String) : CareHistoryEvent
    data class DraftPlantSelected(val plantId: String) : CareHistoryEvent
    data class DraftCareTypeSelected(val careType: CareType) : CareHistoryEvent
    data class DraftPerformedAtSelected(val performedAtOption: CareHistoryPerformedAtOption) : CareHistoryEvent
    data class DraftNotesChanged(val value: String) : CareHistoryEvent
    data class DraftPhotoUriChanged(val value: String) : CareHistoryEvent
}

sealed interface CareHistoryEffect {
    data object NavigateBack : CareHistoryEffect
    data object OpenPhotoPicker : CareHistoryEffect
}

private data class CareHistoryFilterState(
    val selectedPlantId: String? = null,
    val selectedCareType: CareType? = null,
    val selectedDateRange: CareHistoryDateRange = CareHistoryDateRange.ALL_TIME,
)

private data class CareHistoryActionState(
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val pendingDeleteEntryId: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class CareHistoryViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val plantRepository = WaterMeAppContainer.plantRepository(appContext)
    private val careRepository = WaterMeAppContainer.careRepository(appContext)
    private val clock = Clock.systemDefaultZone()

    private val initialPlantId = savedStateHandle
        .get<String>(WaterMeRoute.CareHistory.PLANT_ID_ARG)
        ?.takeIf { it.isNotBlank() }
    private val filters = MutableStateFlow(CareHistoryFilterState(selectedPlantId = initialPlantId))
    private val draft = MutableStateFlow(CareHistoryDraftUiState(plantId = initialPlantId.orEmpty()))
    private val actionState = MutableStateFlow(CareHistoryActionState())
    private val _effects = MutableSharedFlow<CareHistoryEffect>()
    private var messageDismissJob: Job? = null

    val effects = _effects.asSharedFlow()

    private val historyEntries = filters.flatMapLatest { filter ->
        val (startMillis, endMillis) = filter.selectedDateRange.bounds(clock)
        careRepository.observeFilteredCareHistoryForUser(
            userId = WaterMeAppContainer.LOCAL_USER_ID,
            plantId = filter.selectedPlantId,
            careType = filter.selectedCareType,
            startMillis = startMillis,
            endMillis = endMillis,
        )
    }.map { entries -> entries.map { it.toCareHistoryUiModel(clock) } }

    private val plantOptions = plantRepository
        .observePlantsWithDetails(WaterMeAppContainer.LOCAL_USER_ID)
        .map { plants ->
            plants
                .map { PlantFilterUiModel(id = it.plant.plantId, name = it.plant.name) }
                .sortedBy { it.name.lowercase() }
        }

    val uiState = combine(
        historyEntries,
        plantOptions,
        filters,
        draft,
        actionState,
    ) { entries, plants, filter, draftState, action ->
        CareHistoryUiState(
            isLoading = false,
            isSaving = action.isSaving,
            isDeleting = action.isDeleting,
            selectedPlantId = filter.selectedPlantId,
            selectedCareType = filter.selectedCareType,
            selectedDateRange = filter.selectedDateRange,
            plantOptions = plants,
            entries = entries,
            draft = draftState,
            pendingDeleteEntryId = action.pendingDeleteEntryId,
            errorMessage = action.errorMessage,
            successMessage = action.successMessage,
        )
    }
        .catch { error -> emit(CareHistoryUiState(errorMessage = error.toUserMessage())) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CareHistoryUiState(isLoading = true),
        )

    init {
        seedIfEmpty()
    }

    fun onEvent(event: CareHistoryEvent) {
        when (event) {
            CareHistoryEvent.BackClicked -> emitEffect(CareHistoryEffect.NavigateBack)
            CareHistoryEvent.RetryClicked -> {
                actionState.value = CareHistoryActionState()
                seedIfEmpty()
            }
            CareHistoryEvent.AddManualEntryClicked -> showManualEntryDraft()
            CareHistoryEvent.DismissDraftClicked -> draft.value = CareHistoryDraftUiState(plantId = initialPlantId.orEmpty())
            CareHistoryEvent.SaveDraftClicked -> saveDraft()
            CareHistoryEvent.DraftPhotoPickerClicked -> emitEffect(CareHistoryEffect.OpenPhotoPicker)
            CareHistoryEvent.ClearFiltersClicked -> filters.value = CareHistoryFilterState()
            CareHistoryEvent.DismissDeleteClicked -> actionState.value = CareHistoryActionState()
            CareHistoryEvent.ConfirmDeleteClicked -> deletePendingEntry()
            is CareHistoryEvent.PlantFilterSelected -> filters.update { it.copy(selectedPlantId = event.plantId) }
            is CareHistoryEvent.CareTypeFilterSelected -> filters.update { it.copy(selectedCareType = event.careType) }
            is CareHistoryEvent.DateRangeSelected -> filters.update { it.copy(selectedDateRange = event.dateRange) }
            is CareHistoryEvent.EditEntryClicked -> editEntry(event.entry)
            is CareHistoryEvent.DeleteEntryClicked -> {
                actionState.value = CareHistoryActionState(pendingDeleteEntryId = event.historyId)
            }
            is CareHistoryEvent.DraftPlantSelected -> draft.update { it.copy(plantId = event.plantId, errorMessage = null) }
            is CareHistoryEvent.DraftCareTypeSelected -> draft.update { it.copy(careType = event.careType) }
            is CareHistoryEvent.DraftPerformedAtSelected -> {
                draft.update { it.copy(performedAtOption = event.performedAtOption) }
            }
            is CareHistoryEvent.DraftNotesChanged -> updateDraftNotes(event.value)
            is CareHistoryEvent.DraftPhotoUriChanged -> updateDraftPhoto(event.value)
        }
    }

    fun onPhotoSelected(photoUri: String?) {
        draft.update { it.copy(photoUri = photoUri.orEmpty(), errorMessage = null) }
    }

    private fun showManualEntryDraft() {
        val plantId = filters.value.selectedPlantId
            ?: initialPlantId
            ?: uiState.value.plantOptions.firstOrNull()?.id.orEmpty()
        draft.value = CareHistoryDraftUiState(
            isVisible = true,
            plantId = plantId,
        )
        actionState.value = CareHistoryActionState()
    }

    private fun editEntry(entry: CareHistoryUiModel) {
        draft.value = CareHistoryDraftUiState(
            isVisible = true,
            editingEntryId = entry.id,
            plantId = entry.plantId,
            careType = entry.careType,
            performedAtOption = CareHistoryPerformedAtOption.fromMillis(entry.performedAtMillis, clock),
            notes = entry.notes,
            photoUri = entry.photoUri.orEmpty(),
        )
        actionState.value = CareHistoryActionState()
    }

    private fun updateDraftNotes(value: String) {
        draft.update { state ->
            state.copy(
                notes = value.take(MAX_NOTES_LENGTH),
                errorMessage = if (value.length > MAX_NOTES_LENGTH) {
                    "Care notes are limited to $MAX_NOTES_LENGTH characters."
                } else {
                    null
                },
            )
        }
    }

    private fun updateDraftPhoto(value: String) {
        draft.update { state ->
            state.copy(
                photoUri = value.take(MAX_PHOTO_URI_LENGTH),
                errorMessage = if (value.length > MAX_PHOTO_URI_LENGTH) {
                    "Photo links are limited to $MAX_PHOTO_URI_LENGTH characters."
                } else {
                    null
                },
            )
        }
    }

    private fun saveDraft() {
        val currentDraft = draft.value
        if (currentDraft.plantId.isBlank()) {
            draft.update { it.copy(errorMessage = "Choose a plant before saving this care entry.") }
            return
        }

        viewModelScope.launch {
            actionState.value = CareHistoryActionState(isSaving = true)
            val performedAt = currentDraft.performedAtOption.toMillis(clock)
            val result = runCatching {
                if (currentDraft.editingEntryId == null) {
                    careRepository.logManualCare(
                        plantId = currentDraft.plantId,
                        careType = currentDraft.careType,
                        performedAt = performedAt,
                        notes = currentDraft.notes,
                        photoUri = currentDraft.photoUri,
                    )
                } else {
                    careRepository.updateCareHistoryEntry(
                        historyId = currentDraft.editingEntryId,
                        plantId = currentDraft.plantId,
                        careType = currentDraft.careType,
                        performedAt = performedAt,
                        notes = currentDraft.notes,
                        photoUri = currentDraft.photoUri,
                    )
                }
            }

            result
                .onSuccess {
                    draft.value = CareHistoryDraftUiState(plantId = initialPlantId.orEmpty())
                    showMessage(
                        successMessage = if (currentDraft.isEditing) "Care entry updated." else "Care entry added.",
                    )
                }
                .onFailure { showMessage(errorMessage = it.toUserMessage()) }
        }
    }

    private fun deletePendingEntry() {
        val historyId = actionState.value.pendingDeleteEntryId ?: return
        viewModelScope.launch {
            actionState.value = CareHistoryActionState(isDeleting = true, pendingDeleteEntryId = historyId)
            runCatching { careRepository.deleteCareHistoryEntry(historyId) }
                .onSuccess { showMessage(successMessage = "Care entry deleted.") }
                .onFailure { showMessage(errorMessage = it.toUserMessage()) }
        }
    }

    private fun seedIfEmpty() {
        viewModelScope.launch {
            runCatching { WaterMeAppContainer.seedIfEmpty(appContext) }
                .onFailure { showMessage(errorMessage = it.toUserMessage()) }
        }
    }

    private fun showMessage(
        successMessage: String? = null,
        errorMessage: String? = null,
    ) {
        actionState.value = CareHistoryActionState(successMessage = successMessage, errorMessage = errorMessage)
        messageDismissJob?.cancel()
        messageDismissJob = viewModelScope.launch {
            delay(MESSAGE_VISIBLE_MILLIS)
            if (actionState.value.successMessage == successMessage && actionState.value.errorMessage == errorMessage) {
                actionState.value = actionState.value.copy(successMessage = null, errorMessage = null)
            }
        }
    }

    private fun emitEffect(effect: CareHistoryEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }

    private fun Throwable.toUserMessage(): String =
        message?.takeIf { it.isNotBlank() } ?: "WaterMe could not load care history."

    private companion object {
        const val MAX_NOTES_LENGTH = 320
        const val MAX_PHOTO_URI_LENGTH = 500
        const val MESSAGE_VISIBLE_MILLIS = 2_400L
    }
}
