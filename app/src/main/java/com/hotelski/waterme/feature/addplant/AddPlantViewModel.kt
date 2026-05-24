package com.hotelski.waterme.feature.addplant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hotelski.waterme.appstate.WaterMeAppContainer
import com.hotelski.waterme.feature.common.ReminderDraftUiModel
import com.hotelski.waterme.feature.common.daysFromTodayMillis
import com.hotelski.waterme.model.CareType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AddPlantEffect {
    data object NavigateBack : AddPlantEffect
    data object OpenPhotoPicker : AddPlantEffect
    data class NavigateToPlantDetails(val plantId: String) : AddPlantEffect
}

class AddPlantViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val plantRepository = WaterMeAppContainer.plantRepository(appContext)
    private val reminderRepository = WaterMeAppContainer.reminderRepository(appContext)

    private val _uiState = MutableStateFlow(AddPlantUiState())
    private val _effects = MutableSharedFlow<AddPlantEffect>()

    val uiState = _uiState.asStateFlow()
    val effects = _effects.asSharedFlow()

    fun onEvent(event: AddPlantEvent) {
        when (event) {
            AddPlantEvent.BackClicked -> emitEffect(AddPlantEffect.NavigateBack)
            AddPlantEvent.ChoosePhotoClicked -> emitEffect(AddPlantEffect.OpenPhotoPicker)
            AddPlantEvent.SaveClicked -> savePlant()
            is AddPlantEvent.NameChanged -> updateField { copy(name = event.value, fieldErrors = fieldErrors.copy(name = null)) }
            is AddPlantEvent.PlantTypeChanged -> updateField { copy(plantType = event.value) }
            is AddPlantEvent.LocationChanged -> updateField { copy(location = event.value) }
            is AddPlantEvent.NotesChanged -> updateField { copy(notes = event.value) }
            is AddPlantEvent.ReminderEnabledChanged -> updateReminder(event.careType) { copy(enabled = event.enabled) }
            is AddPlantEvent.ReminderEveryDaysChanged -> {
                updateReminder(event.careType) { copy(everyDays = event.value.filter { it.isDigit() }) }
            }
            is AddPlantEvent.ReminderStartsInChanged -> {
                updateReminder(event.careType) { copy(startsInDays = event.value.filter { it.isDigit() }) }
            }
            AddPlantEvent.RetryClicked -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    fun onPhotoSelected(uri: String?) {
        _uiState.update { it.copy(selectedPhotoUri = uri?.takeIf { value -> value.isNotBlank() }) }
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
                val plantId = plantRepository.addPlant(
                    userId = WaterMeAppContainer.LOCAL_USER_ID,
                    name = current.name,
                    plantType = current.plantType,
                    location = current.location,
                    notes = current.notes,
                    primaryPhotoUri = current.selectedPhotoUri,
                )

                current.reminders
                    .filter { it.enabled }
                    .forEach { reminder ->
                        reminderRepository.addReminder(
                            plantId = plantId,
                            careType = reminder.careType,
                            frequencyDays = reminder.everyDays.toInt(),
                            nextDueAt = daysFromTodayMillis(reminder.startsInDays.toLong()),
                            preferredHour = DEFAULT_REMINDER_HOUR,
                            preferredMinute = DEFAULT_REMINDER_MINUTE,
                            notificationsEnabled = true,
                        )
                    }
                plantId
            }
                .onSuccess { plantId ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            successMessage = "Plant saved.",
                            createdPlantId = plantId,
                        )
                    }
                    _effects.emit(AddPlantEffect.NavigateToPlantDetails(plantId))
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

    private fun validate(state: AddPlantUiState): AddPlantFieldErrors? {
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
            AddPlantFieldErrors(name = nameError, reminders = reminderErrors)
        }
    }

    private fun updateField(reducer: AddPlantUiState.() -> AddPlantUiState) {
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

    private fun emitEffect(effect: AddPlantEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }

    private fun Throwable.toUserMessage(): String =
        message?.takeIf { it.isNotBlank() } ?: "WaterMe could not save this plant."

    private companion object {
        const val DEFAULT_REMINDER_HOUR = 9
        const val DEFAULT_REMINDER_MINUTE = 0
        const val MAX_NAME_LENGTH = 80
        const val MAX_REMINDER_DAYS = 365
    }
}
