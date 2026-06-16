package com.hotelski.waterme.feature.addplant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hotelski.waterme.appstate.WaterMeAppContainer
import com.hotelski.waterme.model.CareType
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
    private val reminderNotifications = WaterMeAppContainer.reminderNotificationCoordinator(appContext)

    private val _uiState = MutableStateFlow(defaultUiState())
    private val _effects = MutableSharedFlow<AddPlantEffect>()

    val uiState = _uiState.asStateFlow()
    val effects = _effects.asSharedFlow()

    fun onEvent(event: AddPlantEvent) {
        when (event) {
            AddPlantEvent.BackClicked -> emitEffect(AddPlantEffect.NavigateBack)
            AddPlantEvent.ChoosePhotoClicked -> emitEffect(AddPlantEffect.OpenPhotoPicker)
            AddPlantEvent.SaveClicked -> savePlant()
            AddPlantEvent.DismissStartDatePicker -> updateField { copy(startDatePickerCareType = null) }
            is AddPlantEvent.StartDateSelected -> updateStartDate(event.millis)
            is AddPlantEvent.NameChanged -> updateField {
                copy(
                    name = event.value.take(MAX_NAME_LENGTH),
                    fieldErrors = fieldErrors.copy(name = null),
                )
            }
            is AddPlantEvent.EnvironmentSelected -> updateField { copy(environment = event.environment) }
            is AddPlantEvent.NotesChanged -> updateField { copy(notes = event.value.take(MAX_NOTES_LENGTH)) }
            is AddPlantEvent.ReminderEnabledChanged -> updateReminder(event.careType) {
                copy(enabled = event.enabled)
            }
            is AddPlantEvent.FrequencySelected -> updateReminder(event.careType) {
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
                )
            }
            is AddPlantEvent.CustomFrequencyDaysChanged -> updateReminder(event.careType) {
                copy(
                    customFrequencyDays = event.value.filter { it.isDigit() }.take(MAX_FREQUENCY_DIGITS),
                )
            }
            is AddPlantEvent.ReminderTimeSelected -> updateReminder(event.careType) {
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
                )
            }
            is AddPlantEvent.CustomReminderHourChanged -> updateReminder(event.careType) {
                copy(
                    customReminderHour = event.value.filter { it.isDigit() }.take(MAX_TIME_DIGITS),
                )
            }
            is AddPlantEvent.CustomReminderMinuteChanged -> updateReminder(event.careType) {
                copy(
                    customReminderMinute = event.value.filter { it.isDigit() }.take(MAX_TIME_DIGITS),
                )
            }
            is AddPlantEvent.StartDateClicked -> updateField {
                copy(startDatePickerCareType = event.careType)
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

    fun applyPrefill(
        name: String?,
        photoUri: String?,
    ) {
        val normalizedName = name
            ?.trim()
            ?.take(MAX_NAME_LENGTH)
            .orEmpty()
        val normalizedPhotoUri = photoUri?.takeIf { it.isNotBlank() }
        if (normalizedName.isBlank() && normalizedPhotoUri == null) {
            return
        }

        _uiState.update {
            it.copy(
                name = normalizedName.ifBlank { it.name },
                selectedPhotoUri = normalizedPhotoUri ?: it.selectedPhotoUri,
                fieldErrors = it.fieldErrors.copy(name = null),
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
                val enabledReminders = current.reminders.filter { it.enabled }
                val plantId = plantRepository.addPlant(
                    userId = WaterMeAppContainer.LOCAL_USER_ID,
                    name = plantName,
                    plantType = DEFAULT_PLANT_TYPE,
                    location = DEFAULT_PLANT_LOCATION,
                    environment = current.environment,
                    notes = current.notes,
                    primaryPhotoUri = current.selectedPhotoUri,
                )

                enabledReminders.forEach { reminder ->
                    val frequencyDays = reminder.resolvedFrequencyDays()
                    val reminderHour = reminder.resolvedReminderHour()
                    val reminderMinute = reminder.resolvedReminderMinute()
                    val dueAtMillis = reminder.startDateMillis.toDueAtMillis(reminderHour, reminderMinute)
                    reminderRepository.addReminder(
                        plantId = plantId,
                        careType = reminder.careType,
                        frequencyDays = frequencyDays,
                        nextDueAt = dueAtMillis,
                        preferredHour = reminderHour,
                        preferredMinute = reminderMinute,
                        notificationsEnabled = true,
                    )
                }
                reminderNotifications.syncScheduledReminders()
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
        val enabledReminders = state.reminders.filter { it.enabled }
        val reminderError = when {
            enabledReminders.isEmpty() -> "Enable watering, fertilizing, or both."
            else -> enabledReminders.firstNotNullOfOrNull { reminder ->
                val label = reminder.careType.label
                when {
                    reminder.careType !in ReminderCareTypes -> "$label is not available for the first schedule."
                    reminder.resolvedFrequencyDays() !in 1..MAX_REMINDER_DAYS ->
                        "$label: choose a frequency between 1 and $MAX_REMINDER_DAYS days."
                    reminder.resolvedReminderHour() !in 0..23 -> "$label: choose an hour between 0 and 23."
                    reminder.resolvedReminderMinute() !in 0..59 -> "$label: choose minutes between 0 and 59."
                    reminder.startDateMillis < todayStartMillis() -> "$label: choose today or a future start date."
                    else -> null
                }
            }
        }

        return if (nameError == null && reminderError == null) {
            null
        } else {
            AddPlantFieldErrors(name = nameError, reminder = reminderError)
        }
    }

    private fun updateStartDate(millis: Long?) {
        val careType = _uiState.value.startDatePickerCareType ?: return
        val selectedDateMillis = millis?.toLocalStartMillis()
            ?: _uiState.value.reminders.firstOrNull { it.careType == careType }?.startDateMillis
            ?: todayStartMillis()

        updateReminder(careType) {
            copy(
                startDateMillis = selectedDateMillis,
                startDateLabel = selectedDateMillis.toDateLabel(),
            )
        }
        _uiState.update {
            it.copy(
                startDatePickerCareType = null,
                fieldErrors = it.fieldErrors.copy(reminder = null),
                errorMessage = null,
                successMessage = null,
            )
        }
    }

    private fun updateField(reducer: AddPlantUiState.() -> AddPlantUiState) {
        _uiState.update { it.reducer().copy(errorMessage = null, successMessage = null) }
    }

    private fun updateReminder(
        careType: CareType,
        reducer: AddPlantReminderDraftUiModel.() -> AddPlantReminderDraftUiModel,
    ) {
        _uiState.update { state ->
            state.copy(
                reminders = state.reminders.map { reminder ->
                    if (reminder.careType == careType) reminder.reducer() else reminder
                },
                fieldErrors = state.fieldErrors.copy(reminder = null),
                errorMessage = null,
                successMessage = null,
            )
        }
    }

    private fun AddPlantReminderDraftUiModel.resolvedFrequencyDays(): Int =
        if (frequency == AddPlantFrequencyOption.CUSTOM) {
            customFrequencyDays.toIntOrNull() ?: 0
        } else {
            frequency.days
        }

    private fun AddPlantReminderDraftUiModel.resolvedReminderHour(): Int =
        if (reminderTime == AddPlantReminderTimeOption.CUSTOM) {
            customReminderHour.toIntOrNull() ?: -1
        } else {
            reminderTime.hour
        }

    private fun AddPlantReminderDraftUiModel.resolvedReminderMinute(): Int =
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
            reminders = ReminderCareTypes.map { careType ->
                careType.toDefaultReminderDraft(todayMillis)
            },
        )
    }

    private fun CareType.toDefaultReminderDraft(todayMillis: Long): AddPlantReminderDraftUiModel {
        val startDateMillis = if (this == CareType.WATERING) {
            todayMillis
        } else {
            LocalDate.now()
                .plusDays(defaultFrequencyDays.toLong())
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }
        val frequency = defaultFrequencyOption()
        return AddPlantReminderDraftUiModel(
            careType = this,
            enabled = this == CareType.WATERING,
            frequency = frequency,
            customFrequencyDays = if (frequency == AddPlantFrequencyOption.CUSTOM) {
                defaultFrequencyDays.toString()
            } else {
                ""
            },
            startDateMillis = startDateMillis,
            startDateLabel = startDateMillis.toDateLabel(),
        )
    }

    private fun CareType.defaultFrequencyOption(): AddPlantFrequencyOption =
        AddPlantFrequencyOption.entries.firstOrNull { it.days == defaultFrequencyDays }
            ?: AddPlantFrequencyOption.EVERY_3_DAYS

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
        val ReminderCareTypes = listOf(CareType.WATERING, CareType.FERTILIZING)
        val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    }
}
