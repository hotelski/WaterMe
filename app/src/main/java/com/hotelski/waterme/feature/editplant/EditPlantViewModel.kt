package com.hotelski.waterme.feature.editplant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.hotelski.waterme.appstate.WaterMeAppContainer
import com.hotelski.waterme.data.local.entity.ReminderEntity
import com.hotelski.waterme.data.local.model.PlantWithDetails
import com.hotelski.waterme.feature.common.ReminderDraftUiModel
import com.hotelski.waterme.feature.common.daysFromTodayMillis
import com.hotelski.waterme.feature.common.startOfTodayMillis
import com.hotelski.waterme.model.CareType
import com.hotelski.waterme.navigation.WaterMeRoute
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface EditPlantEffect {
    data object NavigateBack : EditPlantEffect
    data object OpenPhotoPicker : EditPlantEffect
    data class NavigateToPlantDetails(val plantId: String) : EditPlantEffect
    data object NavigateToPlantsAfterDelete : EditPlantEffect
}

class EditPlantViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val plantRepository = WaterMeAppContainer.plantRepository(appContext)
    private val reminderRepository = WaterMeAppContainer.reminderRepository(appContext)

    private val plantId: String = checkNotNull(savedStateHandle[WaterMeRoute.EditPlant.PLANT_ID_ARG])
    private val _uiState = MutableStateFlow(EditPlantUiState(isLoading = true))
    private val _effects = MutableSharedFlow<EditPlantEffect>()
    private var loadedPrimaryPhotoUri: String? = null

    val uiState = _uiState.asStateFlow()
    val effects = _effects.asSharedFlow()

    init {
        loadPlant()
    }

    fun onEvent(event: EditPlantEvent) {
        when (event) {
            EditPlantEvent.BackClicked -> emitEffect(EditPlantEffect.NavigateBack)
            EditPlantEvent.ChangePhotoClicked -> emitEffect(EditPlantEffect.OpenPhotoPicker)
            EditPlantEvent.SaveClicked -> savePlant()
            EditPlantEvent.DeleteClicked -> deletePlant()
            EditPlantEvent.RetryClicked -> loadPlant()
            is EditPlantEvent.NameChanged -> updateField { copy(name = event.value, fieldErrors = fieldErrors.copy(name = null)) }
            is EditPlantEvent.PlantTypeChanged -> updateField { copy(plantType = event.value) }
            is EditPlantEvent.LocationChanged -> updateField { copy(location = event.value) }
            is EditPlantEvent.NotesChanged -> updateField { copy(notes = event.value) }
            is EditPlantEvent.ReminderEnabledChanged -> updateReminder(event.careType) { copy(enabled = event.enabled) }
            is EditPlantEvent.ReminderEveryDaysChanged -> {
                updateReminder(event.careType) { copy(everyDays = event.value.filter { it.isDigit() }) }
            }
            is EditPlantEvent.ReminderStartsInChanged -> {
                updateReminder(event.careType) { copy(startsInDays = event.value.filter { it.isDigit() }) }
            }
        }
    }

    fun onPhotoSelected(uri: String?) {
        _uiState.update {
            it.copy(
                primaryPhotoUri = uri?.takeIf { value -> value.isNotBlank() } ?: it.primaryPhotoUri,
                errorMessage = null,
                successMessage = null,
            )
        }
    }

    private fun loadPlant() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            runCatching {
                plantRepository.observePlantWithDetails(plantId).first()
            }
                .onSuccess { details ->
                    if (details == null) {
                        _uiState.value = EditPlantUiState(errorMessage = "Plant not found.")
                    } else {
                        loadedPrimaryPhotoUri = details.primaryPhotoUri()
                        _uiState.value = details.toEditPlantUiState()
                    }
                }
                .onFailure { error ->
                    _uiState.value = EditPlantUiState(errorMessage = error.toUserMessage())
                }
        }
    }

    private fun savePlant() {
        val current = _uiState.value
        val validation = validate(current)
        if (validation != null) {
            _uiState.update { it.copy(fieldErrors = validation, errorMessage = null, successMessage = null) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
            runCatching {
                plantRepository.updatePlant(
                    plantId = plantId,
                    name = current.name,
                    plantType = current.plantType,
                    location = current.location,
                    notes = current.notes,
                )

                if (current.primaryPhotoUri != loadedPrimaryPhotoUri) {
                    plantRepository.replacePrimaryPhoto(plantId, current.primaryPhotoUri)
                }

                current.reminders.forEach { reminder ->
                    val frequencyDays = reminder.everyDays.toIntOrNull() ?: reminder.careType.defaultFrequencyDays
                    val startsInDays = reminder.startsInDays.toLongOrNull() ?: frequencyDays.toLong()
                    reminderRepository.saveReminderForPlant(
                        plantId = plantId,
                        careType = reminder.careType,
                        enabled = reminder.enabled,
                        frequencyDays = frequencyDays,
                        nextDueAt = daysFromTodayMillis(startsInDays),
                        preferredHour = DEFAULT_REMINDER_HOUR,
                        preferredMinute = DEFAULT_REMINDER_MINUTE,
                        notificationsEnabled = true,
                    )
                }
            }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            successMessage = "Plant updated.",
                        )
                    }
                    _effects.emit(EditPlantEffect.NavigateToPlantDetails(plantId))
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    private fun deletePlant() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, errorMessage = null, successMessage = null) }
            runCatching { plantRepository.deletePlant(plantId) }
                .onSuccess {
                    _uiState.update { it.copy(isDeleting = false, successMessage = "Plant deleted.") }
                    _effects.emit(EditPlantEffect.NavigateToPlantsAfterDelete)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            errorMessage = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    private fun validate(state: EditPlantUiState): EditPlantFieldErrors? {
        val nameError = when {
            state.name.isBlank() -> "Plant name is required."
            state.name.length > MAX_NAME_LENGTH -> "Plant name is too long."
            else -> null
        }

        val reminderErrors = state.reminders
            .filter { it.enabled }
            .mapNotNull { reminder ->
                val everyDays = reminder.everyDays.toIntOrNull()
                val startsInDays = reminder.startsInDays.toIntOrNull()
                val error = when {
                    everyDays == null || everyDays !in 1..MAX_REMINDER_DAYS -> "Use 1-$MAX_REMINDER_DAYS days."
                    startsInDays == null || startsInDays !in 0..MAX_REMINDER_DAYS -> "Start in 0-$MAX_REMINDER_DAYS days."
                    else -> null
                }
                error?.let { reminder.careType to it }
            }
            .toMap()

        return if (nameError == null && reminderErrors.isEmpty()) {
            null
        } else {
            EditPlantFieldErrors(name = nameError, reminders = reminderErrors)
        }
    }

    private fun updateField(reducer: EditPlantUiState.() -> EditPlantUiState) {
        _uiState.update { it.reducer().copy(errorMessage = null, successMessage = null) }
    }

    private fun updateReminder(
        careType: CareType,
        reducer: ReminderDraftUiModel.() -> ReminderDraftUiModel,
    ) {
        _uiState.update { state ->
            state.copy(
                reminders = state.reminders.map { reminder ->
                    if (reminder.careType == careType) reminder.reducer() else reminder
                },
                fieldErrors = state.fieldErrors.copy(reminders = state.fieldErrors.reminders - careType),
                errorMessage = null,
                successMessage = null,
            )
        }
    }

    private fun PlantWithDetails.toEditPlantUiState(): EditPlantUiState {
        val remindersByType = reminders.associateBy { it.careType }
        return EditPlantUiState(
            name = plant.name,
            plantType = plant.plantType,
            location = plant.location,
            notes = plant.notes,
            primaryPhotoUri = primaryPhotoUri(),
            reminders = CareType.entries.map { careType -> remindersByType.toReminderDraft(careType) },
        )
    }

    private fun Map<CareType, ReminderEntity>.toReminderDraft(careType: CareType): ReminderDraftUiModel {
        val reminder = this[careType]
        return ReminderDraftUiModel(
            careType = careType,
            enabled = reminder?.isEnabled ?: false,
            everyDays = (reminder?.frequencyDays ?: careType.defaultFrequencyDays).toString(),
            startsInDays = reminder?.nextDueAt?.daysUntilDue()?.toString() ?: careType.defaultFrequencyDays.toString(),
        )
    }

    private fun PlantWithDetails.primaryPhotoUri(): String? =
        photos.firstOrNull { it.isPrimary }?.localUri ?: photos.firstOrNull()?.localUri

    private fun Long.daysUntilDue(): Long {
        val millisUntilDue = this - startOfTodayMillis()
        return (millisUntilDue / DAY_MILLIS).coerceAtLeast(0L)
    }

    private fun emitEffect(effect: EditPlantEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }

    private fun Throwable.toUserMessage(): String =
        message?.takeIf { it.isNotBlank() } ?: "WaterMe could not update this plant."

    private companion object {
        const val DEFAULT_REMINDER_HOUR = 9
        const val DEFAULT_REMINDER_MINUTE = 0
        const val MAX_NAME_LENGTH = 80
        const val MAX_REMINDER_DAYS = 365
        const val DAY_MILLIS = 86_400_000L
    }
}
