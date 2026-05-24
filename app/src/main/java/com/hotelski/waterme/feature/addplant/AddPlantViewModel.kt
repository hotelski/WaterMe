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
                    customFrequencyDays = if (
                        event.frequency == AddPlantFrequencyOption.CUSTOM &&
                        customFrequencyDays.isBlank()
                    ) {
                        frequency.days.coerceAtLeast(1).toString()
                    } else {
                        customFrequencyDays
                    },
                    fieldErrors = fieldErrors.copy(reminder = null),
                )
            }
            is AddPlantEvent.CustomFrequencyDaysChanged -> updateField {
                copy(
                    customFrequencyDays = event.value.filter { it.isDigit() }.take(MAX_FREQUENCY_DIGITS),
                    fieldErrors = fieldErrors.copy(reminder = null),
                )
            }
            is AddPlantEvent.ReminderTimeSelected -> updateField {
                copy(
                    reminderTime = event.time,
                    customReminderHour = if (
                        event.time == AddPlantReminderTimeOption.CUSTOM &&
                        customReminderHour.isBlank()
                    ) {
                        reminderTime.hour.coerceAtLeast(0).toString()
                    } else {
                        customReminderHour
                    },
                    customReminderMinute = if (
                        event.time == AddPlantReminderTimeOption.CUSTOM &&
                        customReminderMinute.isBlank()
                    ) {
                        reminderTime.minute.coerceAtLeast(0).toString().padStart(2, '0')
                    } else {
                        customReminderMinute
                    },
                    fieldErrors = fieldErrors.copy(reminder = null),
                )
            }
            is AddPlantEvent.CustomReminderHourChanged -> updateField {
                copy(
                    customReminderHour = event.value.filter { it.isDigit() }.take(MAX_TIME_DIGITS),
                    fieldErrors = fieldErrors.copy(reminder = null),
                )
            }
            is AddPlantEvent.CustomReminderMinuteChanged -> updateField {
                copy(
                    customReminderMinute = event.value.filter { it.isDigit() }.take(MAX_TIME_DIGITS),
                    fieldErrors = fieldErrors.copy(reminder = null),
                )
            }
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
                val frequencyDays = current.resolvedFrequencyDays()
                val reminderHour = current.resolvedReminderHour()
                val reminderMinute = current.resolvedReminderMinute()
                val dueAtMillis = current.startDateMillis.toDueAtMillis(reminderHour, reminderMinute)
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
                    frequencyDays = frequencyDays,
                    nextDueAt = dueAtMillis,
                    preferredHour = reminderHour,
                    preferredMinute = reminderMinute,
                    notificationsEnabled = true,
                )
                scheduleInitialReminder(
                    plantId = plantId,
                    plantName = plantName,
                    reminderId = reminderId,
                    careType = current.reminderCareType,
                    dueAtMillis = dueAtMillis,
                    frequencyDays = frequencyDays,
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
            state.resolvedFrequencyDays() !in 1..MAX_REMINDER_DAYS -> "Choose a frequency between 1 and $MAX_REMINDER_DAYS days."
            state.resolvedReminderHour() !in 0..23 -> "Choose an hour between 0 and 23."
            state.resolvedReminderMinute() !in 0..59 -> "Choose minutes between 0 and 59."
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

    private fun AddPlantUiState.resolvedFrequencyDays(): Int =
        if (frequency == AddPlantFrequencyOption.CUSTOM) {
            customFrequencyDays.toIntOrNull() ?: 0
        } else {
            frequency.days
        }

    private fun AddPlantUiState.resolvedReminderHour(): Int =
        if (reminderTime == AddPlantReminderTimeOption.CUSTOM) {
            customReminderHour.toIntOrNull() ?: -1
        } else {
            reminderTime.hour
        }

    private fun AddPlantUiState.resolvedReminderMinute(): Int =
        if (reminderTime == AddPlantReminderTimeOption.CUSTOM) {
            customReminderMinute.toIntOrNull() ?: -1
        } else {
            reminderTime.minute
        }

    private fun Long.toDueAtMillis(hour: Int, minute: Int): Long =
        Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .atTime(hour, minute)
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
        const val MAX_REMINDER_DAYS = 365
        const val MAX_FREQUENCY_DIGITS = 3
        const val MAX_TIME_DIGITS = 2
        val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    }
}
