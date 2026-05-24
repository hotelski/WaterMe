package com.hotelski.waterme.feature.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hotelski.waterme.appstate.WaterMeAppContainer
import com.hotelski.waterme.data.local.entity.BackupSyncProvider
import com.hotelski.waterme.data.local.entity.HistoryAction
import com.hotelski.waterme.data.local.entity.MeasurementUnits
import com.hotelski.waterme.data.local.entity.NotificationPermissionState
import com.hotelski.waterme.data.local.entity.ThemePreference
import com.hotelski.waterme.notifications.NotificationPermissionHelper
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
}

private data class SettingsActionState(
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

class SettingsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val plantRepository = WaterMeAppContainer.plantRepository(appContext)
    private val careRepository = WaterMeAppContainer.careRepository(appContext)
    private val settingsRepository = WaterMeAppContainer.settingsRepository(appContext)

    private val actionState = MutableStateFlow(SettingsActionState())
    private val _effects = MutableSharedFlow<SettingsEffect>()

    val effects = _effects.asSharedFlow()

    val uiState = combine(
        settingsRepository.observeSettings(WaterMeAppContainer.LOCAL_USER_ID),
        plantRepository.observePlantsWithDetails(WaterMeAppContainer.LOCAL_USER_ID),
        careRepository.observeCareHistoryForUser(WaterMeAppContainer.LOCAL_USER_ID),
        actionState,
    ) { settings, plants, careHistory, action ->
        SettingsUiState(
            isLoading = false,
            notificationsEnabled = settings?.notificationsEnabled ?: true,
            notificationPermissionLabel = settings?.notificationPermissionState.toPermissionLabel(),
            themePreference = (settings?.darkModePreference ?: ThemePreference.SYSTEM).toUiPreference(),
            measurementUnits = (settings?.measurementUnits ?: MeasurementUnits.METRIC).toUiUnits(),
            backupSyncEnabled = settings?.backupSyncEnabled ?: false,
            plantCount = plants.size,
            activeReminderCount = plants.sumOf { plant -> plant.reminders.count { it.isEnabled && it.deletedAt == null } },
            careHistoryCount = careHistory.count { it.action != HistoryAction.HEALTH_NOTE },
            healthNoteCount = careHistory.count { it.action == HistoryAction.HEALTH_NOTE },
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
            SettingsEvent.RequestNotificationPermissionClicked -> emitEffect(SettingsEffect.RequestNotificationPermission)
            is SettingsEvent.NotificationsChanged -> updateNotifications(event.enabled)
            is SettingsEvent.ThemePreferenceChanged -> updateTheme(event.preference)
            is SettingsEvent.MeasurementUnitsChanged -> updateUnits(event.units)
            is SettingsEvent.BackupSyncChanged -> updateBackup(event.enabled)
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
                settingsRepository.updateNotificationSettings(
                    userId = WaterMeAppContainer.LOCAL_USER_ID,
                    enabled = enabled,
                    permissionState = permissionState,
                )
            }
                .onSuccess { actionState.value = SettingsActionState(successMessage = successMessage) }
                .onFailure { actionState.value = SettingsActionState(errorMessage = it.toUserMessage()) }
        }
    }

    private fun updateTheme(preference: SettingsThemePreference) {
        viewModelScope.launch {
            runCatching {
                settingsRepository.updateThemePreference(
                    userId = WaterMeAppContainer.LOCAL_USER_ID,
                    themePreference = preference.toDatabasePreference(),
                )
            }
                .onSuccess { actionState.value = SettingsActionState(successMessage = "Appearance updated.") }
                .onFailure { actionState.value = SettingsActionState(errorMessage = it.toUserMessage()) }
        }
    }

    private fun updateUnits(units: SettingsMeasurementUnits) {
        viewModelScope.launch {
            runCatching {
                settingsRepository.updateMeasurementUnits(
                    userId = WaterMeAppContainer.LOCAL_USER_ID,
                    measurementUnits = units.toDatabaseUnits(),
                )
            }
                .onSuccess { actionState.value = SettingsActionState(successMessage = "Units updated.") }
                .onFailure { actionState.value = SettingsActionState(errorMessage = it.toUserMessage()) }
        }
    }

    private fun updateBackup(enabled: Boolean) {
        viewModelScope.launch {
            runCatching {
                settingsRepository.updateBackupSync(
                    userId = WaterMeAppContainer.LOCAL_USER_ID,
                    enabled = enabled,
                    provider = if (enabled) BackupSyncProvider.GOOGLE_DRIVE else BackupSyncProvider.NONE,
                    lastBackupAt = null,
                    lastSyncedAt = null,
                )
            }
                .onSuccess { actionState.value = SettingsActionState(successMessage = "Backup preference updated.") }
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

    private fun NotificationPermissionState?.toPermissionLabel(): String =
        when (this) {
            NotificationPermissionState.GRANTED -> "Granted"
            NotificationPermissionState.DENIED -> "Denied"
            NotificationPermissionState.NOT_REQUESTED, null -> "Not requested"
        }

    private fun ThemePreference.toUiPreference(): SettingsThemePreference =
        when (this) {
            ThemePreference.SYSTEM -> SettingsThemePreference.SYSTEM
            ThemePreference.LIGHT -> SettingsThemePreference.LIGHT
            ThemePreference.DARK -> SettingsThemePreference.DARK
        }

    private fun SettingsThemePreference.toDatabasePreference(): ThemePreference =
        when (this) {
            SettingsThemePreference.SYSTEM -> ThemePreference.SYSTEM
            SettingsThemePreference.LIGHT -> ThemePreference.LIGHT
            SettingsThemePreference.DARK -> ThemePreference.DARK
        }

    private fun MeasurementUnits.toUiUnits(): SettingsMeasurementUnits =
        when (this) {
            MeasurementUnits.METRIC -> SettingsMeasurementUnits.METRIC
            MeasurementUnits.IMPERIAL -> SettingsMeasurementUnits.IMPERIAL
        }

    private fun SettingsMeasurementUnits.toDatabaseUnits(): MeasurementUnits =
        when (this) {
            SettingsMeasurementUnits.METRIC -> MeasurementUnits.METRIC
            SettingsMeasurementUnits.IMPERIAL -> MeasurementUnits.IMPERIAL
        }

    private fun Throwable.toUserMessage(): String =
        message?.takeIf { it.isNotBlank() } ?: "WaterMe could not update settings."
}
