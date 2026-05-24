package com.hotelski.waterme.feature.addplant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hotelski.waterme.appstate.WaterMeAppContainer
import com.hotelski.waterme.model.CareType
import com.hotelski.waterme.notifications.ReminderScheduler
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
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

    private val _uiState = MutableStateFlow(defaultUiState())
    private val _effects = MutableSharedFlow<AddPlantEffect>()

    val uiState = _uiState.asStateFlow()
    val effects = _effects.asSharedFlow()

    fun onEvent(event: AddPlantEvent) {
        when (event) {
            AddPlantEvent.BackClicked -> emitEffect(AddPlantEffect.NavigateBack)
            AddPlantEvent.ChoosePhotoClicked -> emitEffect(AddPlantEffect.OpenPhotoPicker)
            AddPlantEvent.SaveClicked -> savePlant()
            AddPlantEvent.StartDateClicked -> updateField { copy(showStartDatePicker = true) }
            AddPlantEvent.DismissStartDatePicker -> updateField { copy(showStartDatePicker = false) }
            is AddPlantEvent.StartDateSelected -> updateStartDate(event.millis)
            is AddPlantEvent.NameChanged -> updateField {
                copy(
                    name = event.value.take(MAX_NAME_LENGTH),
                    fieldErrors = fieldErrors.copy(name = null),
                )
            }
            is AddPlantEvent.NotesChanged -> updateField { copy(notes = event.value.take(MAX_NOTES_LENGTH)) }
            is AddPlantEvent.ReminderCareTypeSelected -> updateField {
                copy(
                    reminderCareType = event.careType,
                    fieldErrors = fieldErrors.copy(reminder = null),
                )
            }
            is AddPlantEvent.FrequencySelected -> updateField {
                copy(
                    frequency = event.frequency,
                    fieldErrors = fieldErrors.copy(reminder = null),
                )
            }
            is AddPlantEvent.ReminderTimeSelected -> updateField { copy(reminderTime = event.time) }
            AddPlantEvent.RetryClicked -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    fun onPhotoSelected(uri: String?) {
        _uiState.update {
            it.copy(
                selectedPhotoUri = uri?.takeIf { value -> value.isNotBlank() },
                errorMessage = null,
                successMessage = null,
            )
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
                val plantName = current.name.trim()
                val dueAtMillis = current.startDateMillis.toDueAtMillis(current.reminderTime)
                val plantId = plantRepository.addPlant(
                    userId = WaterMeAppContainer.LOCAL_USER_ID,
                    name = plantName,
                    plantType = DEFAULT_PLANT_TYPE,
                    location = DEFAULT_PLANT_LOCATION,
                    notes = current.notes,
                    primaryPhotoUri = current.selectedPhotoUri,
                )
                val reminderId = reminderRepository.addReminder(
                    plantId = plantId,
                    careType = current.reminderCareType,
                    frequencyDays = current.frequency.days,
                    nextDueAt = dueAtMillis,
                    preferredHour = current.reminderTime.hour,
                    preferredMinute = current.reminderTime.minute,
                    notificationsEnabled = true,
                )
                scheduleInitialReminder(
                    plantId = plantId,
                    plantName = plantName,
                    reminderId = reminderId,
                    careType = current.reminderCareType,
                    dueAtMillis = dueAtMillis,
                    frequencyDays = current.frequency.days,
                )
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
        val reminderError = when {
            state.reminderCareType != CareType.WATERING && state.reminderCareType != CareType.FERTILIZING ->
                "Choose watering or fertilizing."
            state.startDateMillis < todayStartMillis() -> "Choose today or a future start date."
            else -> null
        }

        return if (nameError == null && reminderError == null) {
            null
        } else {
            AddPlantFieldErrors(name = nameError, reminder = reminderError)
        }
    }

    private fun updateStartDate(millis: Long?) {
        val selectedDateMillis = millis?.toLocalStartMillis() ?: _uiState.value.startDateMillis
        _uiState.update {
            it.copy(
                startDateMillis = selectedDateMillis,
                startDateLabel = selectedDateMillis.toDateLabel(),
                showStartDatePicker = false,
                fieldErrors = it.fieldErrors.copy(reminder = null),
                errorMessage = null,
                successMessage = null,
            )
        }
    }

    private fun updateField(reducer: AddPlantUiState.() -> AddPlantUiState) {
        _uiState.update { it.reducer().copy(errorMessage = null, successMessage = null) }
    }

    private fun scheduleInitialReminder(
        plantId: String,
        plantName: String,
        reminderId: String,
        careType: CareType,
        dueAtMillis: Long,
        frequencyDays: Int,
    ) {
        when (careType) {
            CareType.WATERING -> ReminderScheduler.scheduleWateringReminder(
                context = appContext,
                plantId = plantId,
                plantName = plantName,
                reminderId = reminderId,
                dueAtMillis = dueAtMillis,
                frequencyDays = frequencyDays,
            )
            CareType.FERTILIZING -> ReminderScheduler.scheduleFertilizingReminder(
                context = appContext,
                plantId = plantId,
                plantName = plantName,
                reminderId = reminderId,
                dueAtMillis = dueAtMillis,
                frequencyDays = frequencyDays,
            )
            else -> Unit
        }
    }

    private fun Long.toDueAtMillis(time: AddPlantReminderTimeOption): Long =
        Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .atTime(time.hour, time.minute)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    private fun Long.toDateLabel(): String {
        val date = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
        val today = LocalDate.now()
        return when (date) {
            today -> "Today"
            today.plusDays(1) -> "Tomorrow"
            else -> date.format(dateFormatter)
        }
    }

    private fun todayStartMillis(): Long =
        LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    private fun Long.toLocalStartMillis(): Long =
        Instant.ofEpochMilli(this)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    private fun defaultUiState(): AddPlantUiState {
        val todayMillis = todayStartMillis()
        return AddPlantUiState(
            startDateMillis = todayMillis,
            startDateLabel = todayMillis.toDateLabel(),
        )
    }

    private fun emitEffect(effect: AddPlantEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }

    private fun Throwable.toUserMessage(): String =
        message?.takeIf { it.isNotBlank() } ?: "WaterMe could not save this plant."

    private companion object {
        const val DEFAULT_PLANT_TYPE = "Houseplant"
        const val DEFAULT_PLANT_LOCATION = "Home"
        const val MAX_NAME_LENGTH = 80
        const val MAX_NOTES_LENGTH = 320
        val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    }
}
