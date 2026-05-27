package com.hotelski.waterme.feature.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hotelski.waterme.BuildConfig
import com.hotelski.waterme.appstate.WaterMeAppContainer
import com.hotelski.waterme.data.local.entity.HistoryAction
import com.hotelski.waterme.data.local.entity.NotificationPermissionState
import com.hotelski.waterme.feature.characters.activePlantCharacter
import com.hotelski.waterme.notifications.NotificationPermissionHelper
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SettingsEffect {
    data object NavigateToOnboarding : SettingsEffect
    data object RequestNotificationPermission : SettingsEffect
    data object NavigateToCharacters : SettingsEffect
}

private data class SettingsActionState(
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showDeleteAllDataConfirmation: Boolean = false,
    val isDeletingAllData: Boolean = false,
)

class SettingsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val plantRepository = WaterMeAppContainer.plantRepository(appContext)
    private val careRepository = WaterMeAppContainer.careRepository(appContext)
    private val settingsDataStore = WaterMeAppContainer.settingsDataStore(appContext)

    private val actionState = MutableStateFlow(SettingsActionState())
    private val _effects = MutableSharedFlow<SettingsEffect>()

    val effects = _effects.asSharedFlow()

    val uiState = combine(
        settingsDataStore.settings,
        plantRepository.observePlantsWithDetails(WaterMeAppContainer.LOCAL_USER_ID),
        careRepository.observeCareHistoryForUser(WaterMeAppContainer.LOCAL_USER_ID),
        actionState,
    ) { settings, plants, careHistory, action ->
        val selectedCharacter = activePlantCharacter(
            careHistory = careHistory,
            selectedCharacterId = settings.selectedCharacterId,
            plantsAddedTotal = plants.size,
            appOpenDayStreak = settings.appOpenDayStreak,
        )
        SettingsUiState(
            isLoading = false,
            selectedCharacterName = selectedCharacter.name,
            notificationsEnabled = settings.notificationsEnabled,
            notificationPermissionLabel = settings.notificationPermissionState.toPermissionLabel(),
            appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            plantCount = plants.size,
            activeReminderCount = plants.sumOf { plant -> plant.reminders.count { it.isEnabled && it.deletedAt == null } },
            careHistoryCount = careHistory.count { it.action != HistoryAction.HEALTH_NOTE },
            healthNoteCount = careHistory.count { it.action == HistoryAction.HEALTH_NOTE },
            showDeleteAllDataConfirmation = action.showDeleteAllDataConfirmation,
            isDeletingAllData = action.isDeletingAllData,
            errorMessage = action.errorMessage,
            successMessage = action.successMessage,
        )
    }
        .catch { error -> emit(SettingsUiState(errorMessage = error.toUserMessage())) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(isLoading = true),
        )

    init {
        seedDatabase()
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            SettingsEvent.ShowOnboardingClicked -> emitEffect(SettingsEffect.NavigateToOnboarding)
            SettingsEvent.CharactersClicked -> emitEffect(SettingsEffect.NavigateToCharacters)
            SettingsEvent.RequestNotificationPermissionClicked -> emitEffect(SettingsEffect.RequestNotificationPermission)
            is SettingsEvent.NotificationsChanged -> updateNotifications(event.enabled)
            SettingsEvent.DeleteAllDataClicked -> actionState.value = actionState.value.copy(showDeleteAllDataConfirmation = true)
            SettingsEvent.DismissDeleteAllDataClicked -> actionState.value = actionState.value.copy(showDeleteAllDataConfirmation = false)
            SettingsEvent.ConfirmDeleteAllDataClicked -> deleteAllData()
            SettingsEvent.RetryClicked -> seedDatabase()
        }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        updateNotificationSettings(
            enabled = granted,
            permissionState = if (granted) NotificationPermissionState.GRANTED else NotificationPermissionState.DENIED,
            successMessage = if (granted) "Notifications enabled." else "Notifications disabled.",
        )
    }

    private fun updateNotifications(enabled: Boolean) {
        val canPostNotifications = NotificationPermissionHelper.canPostNotifications(appContext)
        if (enabled && !canPostNotifications) {
            emitEffect(SettingsEffect.RequestNotificationPermission)
            return
        }

        updateNotificationSettings(
            enabled = enabled,
            permissionState = if (enabled) NotificationPermissionState.GRANTED else NotificationPermissionState.DENIED,
            successMessage = if (enabled) "Notifications enabled." else "Notifications disabled.",
        )
    }

    private fun updateNotificationSettings(
        enabled: Boolean,
        permissionState: NotificationPermissionState,
        successMessage: String,
    ) {
        viewModelScope.launch {
            runCatching {
                settingsDataStore.updateNotificationSettings(
                    enabled = enabled,
                    permissionState = permissionState,
                )
            }
                .onSuccess { actionState.value = SettingsActionState(successMessage = successMessage) }
                .onFailure { actionState.value = SettingsActionState(errorMessage = it.toUserMessage()) }
        }
    }


    private fun deleteAllData() {
        viewModelScope.launch {
            actionState.value = SettingsActionState(
                showDeleteAllDataConfirmation = false,
                isDeletingAllData = true,
            )
            runCatching {
                WaterMeAppContainer.deleteAllData(appContext)
                settingsDataStore.clearAfterDeleteAllData()
            }
                .onSuccess { actionState.value = SettingsActionState(successMessage = "All local WaterMe data was deleted.") }
                .onFailure { actionState.value = SettingsActionState(errorMessage = it.toUserMessage()) }
        }
    }

    private fun seedDatabase() {
        viewModelScope.launch {
            actionState.value = SettingsActionState()
            runCatching { WaterMeAppContainer.seedIfEmpty(appContext) }
                .onFailure { actionState.value = SettingsActionState(errorMessage = it.toUserMessage()) }
        }
    }

    private fun emitEffect(effect: SettingsEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }

    private fun NotificationPermissionState.toPermissionLabel(): String =
        when (this) {
            NotificationPermissionState.GRANTED -> "Granted"
            NotificationPermissionState.DENIED -> "Denied"
            NotificationPermissionState.NOT_REQUESTED -> "Not requested"
        }


    private fun Long?.toTimestampLabel(emptyLabel: String): String =
        this?.let { timestamp ->
            Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .format(timestampFormatter)
        } ?: emptyLabel

    private fun Throwable.toUserMessage(): String =
        message?.takeIf { it.isNotBlank() } ?: "WaterMe could not update settings."

    private companion object {
        val timestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")
    }
}
