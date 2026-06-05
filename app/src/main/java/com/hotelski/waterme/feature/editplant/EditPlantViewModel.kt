package com.hotelski.waterme.feature.editplant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.hotelski.waterme.appstate.WaterMeAppContainer
import com.hotelski.waterme.data.local.entity.ReminderEntity
import com.hotelski.waterme.data.local.model.PlantWithDetails
import com.hotelski.waterme.model.CareType
import com.hotelski.waterme.navigation.WaterMeRoute
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
    private val reminderNotifications = WaterMeAppContainer.reminderNotificationCoordinator(appContext)

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
            EditPlantEvent.DismissStartDatePicker -> updateField { copy(startDatePickerCareType = null) }
            is EditPlantEvent.NameChanged -> updateField { copy(name = event.value, fieldErrors = fieldErrors.copy(name = null)) }
            is EditPlantEvent.PlantTypeChanged -> updateField { copy(plantType = event.value) }
            is EditPlantEvent.LocationChanged -> updateField { copy(location = event.value) }
            is EditPlantEvent.EnvironmentSelected -> updateField { copy(environment = event.environment) }
            is EditPlantEvent.NotesChanged -> updateField { copy(notes = event.value) }
            is EditPlantEvent.ReminderEnabledChanged -> updateReminder(event.careType) { copy(enabled = event.enabled) }
            is EditPlantEvent.ReminderEveryDaysChanged -> {
                updateReminder(event.careType) { copy(everyDays = event.value.filter { it.isDigit() }) }
            }
            is EditPlantEvent.ReminderHourChanged -> {
                updateReminder(event.careType) { copy(preferredHour = event.value.filter { it.isDigit() }.take(MAX_TIME_DIGITS)) }
            }
            is EditPlantEvent.ReminderMinuteChanged -> {
                updateReminder(event.careType) { copy(preferredMinute = event.value.filter { it.isDigit() }.take(MAX_TIME_DIGITS)) }
            }
            is EditPlantEvent.ReminderPeriodSelected -> {
                updateReminder(event.careType) { copy(preferredPeriod = event.period) }
            }
            is EditPlantEvent.ReminderStartDateClicked -> updateField {
                copy(startDatePickerCareType = event.careType)
            }
            is EditPlantEvent.ReminderStartDateSelected -> {
                updateReminderStartDate(event.millis)
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
                    environment = current.environment,
                    notes = current.notes,
                )

                if (current.primaryPhotoUri != loadedPrimaryPhotoUri) {
                    plantRepository.replacePrimaryPhoto(plantId, current.primaryPhotoUri)
                }

                current.reminders.forEach { reminder ->
                    val frequencyDays = reminder.everyDays.toIntOrNull() ?: reminder.careType.defaultFrequencyDays
                    val preferredHour = reminder.resolvedPreferredHour()
                    val preferredMinute = reminder.preferredMinute.toIntOrNull() ?: DEFAULT_REMINDER_MINUTE
                    reminderRepository.saveReminderForPlant(
                        plantId = plantId,
                        careType = reminder.careType,
                        enabled = reminder.enabled,
                        frequencyDays = frequencyDays,
                        nextDueAt = reminder.startDateMillis.toReminderDueAtMillis(preferredHour, preferredMinute),
                        preferredHour = preferredHour,
                        preferredMinute = preferredMinute,
                        notificationsEnabled = true,
                    )
                }

                hiddenReminderTypes.forEach { careType ->
                    reminderRepository.saveReminderForPlant(
                        plantId = plantId,
                        careType = careType,
                        enabled = false,
                        frequencyDays = careType.defaultFrequencyDays,
                        nextDueAt = careType.defaultStartDateMillis(),
                        preferredHour = DEFAULT_REMINDER_HOUR,
                        preferredMinute = DEFAULT_REMINDER_MINUTE,
                        notificationsEnabled = true,
                    )
                }
                reminderNotifications.syncScheduledReminders()
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
            runCatching {
                plantRepository.deletePlant(plantId)
                reminderNotifications.syncScheduledReminders()
            }
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
                val preferredHour = reminder.preferredHour.toIntOrNull()
                val preferredMinute = reminder.preferredMinute.toIntOrNull()
                val error = when {
                    everyDays == null || everyDays !in 1..MAX_REMINDER_DAYS -> "Use 1-$MAX_REMINDER_DAYS days."
                    reminder.startDateMillis < todayStartMillis() -> "Choose today or a future start date."
                    preferredHour == null || preferredHour !in 1..12 -> "Choose an hour between 1 and 12."
                    preferredMinute == null || preferredMinute !in 0..59 -> "Choose minutes between 0 and 59."
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
        reducer: EditReminderDraftUiModel.() -> EditReminderDraftUiModel,
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

    private fun updateReminderStartDate(millis: Long?) {
        val careType = _uiState.value.startDatePickerCareType ?: return
        val selectedDateMillis = millis?.toLocalStartMillis()
            ?: _uiState.value.reminders.firstOrNull { it.careType == careType }?.startDateMillis
            ?: careType.defaultStartDateMillis()
        updateReminder(careType) {
            copy(
                startDateMillis = selectedDateMillis,
                startDateLabel = selectedDateMillis.toDateLabel(),
            )
        }
        _uiState.update { it.copy(startDatePickerCareType = null) }
    }

    private fun PlantWithDetails.toEditPlantUiState(): EditPlantUiState {
        val remindersByType = reminders.associateBy { it.careType }
        return EditPlantUiState(
            name = plant.name,
            plantType = plant.plantType,
            location = plant.location,
            environment = plant.environment,
            notes = plant.notes,
            primaryPhotoUri = primaryPhotoUri(),
            reminders = editableReminderTypes.map { careType -> remindersByType.toReminderDraft(careType) },
        )
    }

    private fun Map<CareType, ReminderEntity>.toReminderDraft(careType: CareType): EditReminderDraftUiModel {
        val reminder = this[careType]
        val startDateMillis = reminder?.nextDueAt?.toLocalStartMillis() ?: careType.defaultStartDateMillis()
        val preferredHour = reminder?.preferredHour ?: DEFAULT_REMINDER_HOUR
        return EditReminderDraftUiModel(
            careType = careType,
            enabled = reminder?.isEnabled ?: false,
            everyDays = (reminder?.frequencyDays ?: careType.defaultFrequencyDays).toString(),
            startDateMillis = startDateMillis,
            startDateLabel = startDateMillis.toDateLabel(),
            preferredHour = preferredHour.toDisplayHour().toString(),
            preferredMinute = (reminder?.preferredMinute ?: DEFAULT_REMINDER_MINUTE).toString().padStart(2, '0'),
            preferredPeriod = if (preferredHour >= 12) EditReminderPeriod.PM else EditReminderPeriod.AM,
        )
    }

    private fun PlantWithDetails.primaryPhotoUri(): String? =
        photos.firstOrNull { it.isPrimary }?.localUri ?: photos.firstOrNull()?.localUri

    private fun CareType.defaultStartDateMillis(): Long =
        LocalDate.now()
            .plusDays(defaultFrequencyDays.toLong())
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    private fun Long.toLocalStartMillis(): Long =
        Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    private fun Long.toReminderDueAtMillis(hour: Int, minute: Int): Long =
        Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .atTime(hour, minute)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    private fun EditReminderDraftUiModel.resolvedPreferredHour(): Int {
        val hour = preferredHour.toIntOrNull() ?: return DEFAULT_REMINDER_HOUR
        return when (preferredPeriod) {
            EditReminderPeriod.AM -> if (hour == 12) 0 else hour
            EditReminderPeriod.PM -> if (hour == 12) 12 else hour + 12
        }
    }

    private fun Int.toDisplayHour(): Int =
        when {
            this == 0 -> 12
            this > 12 -> this - 12
            else -> this
        }

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
        const val MAX_TIME_DIGITS = 2
        val editableReminderTypes = listOf(CareType.WATERING, CareType.FERTILIZING)
        val hiddenReminderTypes = CareType.entries.filterNot { it in editableReminderTypes }
        val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
    }
}
