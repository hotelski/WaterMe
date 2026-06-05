package com.hotelski.waterme.feature.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hotelski.waterme.BuildConfig
import com.hotelski.waterme.appstate.WaterMeAppContainer
import com.hotelski.waterme.data.local.entity.NotificationPermissionState
import com.hotelski.waterme.data.local.entity.ThemePreference
import com.hotelski.waterme.data.preferences.TextColorPreference
import com.hotelski.waterme.feature.characters.activePlantCharacter
import com.hotelski.waterme.feature.legal.LegalDocument
import com.hotelski.waterme.notifications.NotificationPermissionHelper
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SettingsEffect {
    data object NavigateToFeedback : SettingsEffect
    data class NavigateToLegal(val document: LegalDocument) : SettingsEffect
    data object RequestNotificationPermission : SettingsEffect
    data object OpenNotificationSettings : SettingsEffect
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
    private val reminderNotifications = WaterMeAppContainer.reminderNotificationCoordinator(appContext)

    private val actionState = MutableStateFlow(SettingsActionState())
    private val _effects = MutableSharedFlow<SettingsEffect>()
    private var messageDismissJob: Job? = null

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
        val canPostNotifications = NotificationPermissionHelper.canPostNotifications(appContext)
        val permissionState = when {
            canPostNotifications -> NotificationPermissionState.GRANTED
            settings.notificationPermissionState == NotificationPermissionState.NOT_REQUESTED ->
                NotificationPermissionState.NOT_REQUESTED
            else -> NotificationPermissionState.DENIED
        }
        SettingsUiState(
            isLoading = false,
            selectedCharacterName = selectedCharacter.name,
            activeCharacter = selectedCharacter,
            notificationsEnabled = settings.notificationsEnabled && canPostNotifications,
            notificationPermissionLabel = permissionState.toPermissionLabel(),
            themePreference = settings.themePreference,
            textColorPreference = settings.textColorPreference,
            appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            plantCount = plants.size,
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
            SettingsEvent.FeedbackClicked -> emitEffect(SettingsEffect.NavigateToFeedback)
            is SettingsEvent.LegalDocumentClicked -> emitEffect(SettingsEffect.NavigateToLegal(event.document))
            SettingsEvent.CharactersClicked -> emitEffect(SettingsEffect.NavigateToCharacters)
            SettingsEvent.RequestNotificationPermissionClicked -> requestNotificationPermission()
            is SettingsEvent.NotificationsChanged -> updateNotifications(event.enabled)
            SettingsEvent.ColorSchemeResetClicked -> resetColorScheme()
            is SettingsEvent.ThemePreferenceChanged -> updateThemePreference(event.value)
            is SettingsEvent.TextColorPreferenceChanged -> updateTextColorPreference(event.value)
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

    private fun requestNotificationPermission() {
        viewModelScope.launch {
            val settings = settingsDataStore.settings.first()
            when {
                NotificationPermissionHelper.canPostNotifications(appContext) -> onNotificationPermissionResult(granted = true)
                settings.notificationPermissionState == NotificationPermissionState.DENIED ->
                    _effects.emit(SettingsEffect.OpenNotificationSettings)
                else -> _effects.emit(SettingsEffect.RequestNotificationPermission)
            }
        }
    }

    private fun updateNotifications(enabled: Boolean) {
        val canPostNotifications = NotificationPermissionHelper.canPostNotifications(appContext)
        if (enabled && !canPostNotifications) {
            requestNotificationPermission()
            return
        }

        updateNotificationSettings(
            enabled = enabled,
            permissionState = if (canPostNotifications) NotificationPermissionState.GRANTED else NotificationPermissionState.DENIED,
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
                if (enabled) {
                    reminderNotifications.syncScheduledReminders()
                } else {
                    reminderNotifications.cancelScheduledReminders()
                }
            }
                .onSuccess { showSuccess(successMessage) }
                .onFailure { showMessage(errorMessage = it.toUserMessage()) }
        }
    }

    private fun updateThemePreference(themePreference: ThemePreference) {
        viewModelScope.launch {
            runCatching { settingsDataStore.updateThemePreference(themePreference) }
                .onSuccess { showSuccess("Color scheme updated.") }
                .onFailure { showMessage(errorMessage = it.toUserMessage()) }
        }
    }

    private fun updateTextColorPreference(textColorPreference: TextColorPreference) {
        viewModelScope.launch {
            runCatching { settingsDataStore.updateTextColorPreference(textColorPreference) }
                .onSuccess { showSuccess("Text color updated.") }
                .onFailure { showMessage(errorMessage = it.toUserMessage()) }
        }
    }

    private fun resetColorScheme() {
        viewModelScope.launch {
            runCatching { settingsDataStore.resetColorScheme() }
                .onSuccess { showSuccess("Color scheme reset.") }
                .onFailure { showMessage(errorMessage = it.toUserMessage()) }
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
                .onSuccess { showSuccess("All local WaterMe data was deleted.") }
                .onFailure { showMessage(errorMessage = it.toUserMessage()) }
        }
    }

    private fun showSuccess(message: String) {
        showMessage(successMessage = message)
    }

    private fun showMessage(
        successMessage: String? = null,
        errorMessage: String? = null,
    ) {
        actionState.value = SettingsActionState(successMessage = successMessage, errorMessage = errorMessage)
        messageDismissJob?.cancel()
        messageDismissJob = viewModelScope.launch {
            delay(SuccessMessageVisibleMillis)
            if (actionState.value.successMessage == successMessage && actionState.value.errorMessage == errorMessage) {
                actionState.value = actionState.value.copy(successMessage = null, errorMessage = null)
            }
        }
    }

    private fun seedDatabase() {
        viewModelScope.launch {
            actionState.value = SettingsActionState()
            runCatching { WaterMeAppContainer.seedIfEmpty(appContext) }
                .onFailure { showMessage(errorMessage = it.toUserMessage()) }
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
        const val SuccessMessageVisibleMillis = 2_400L
    }
}
