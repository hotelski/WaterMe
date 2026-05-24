package com.hotelski.waterme.feature.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hotelski.waterme.BuildConfig
import com.hotelski.waterme.appstate.WaterMeAppContainer
import com.hotelski.waterme.data.local.entity.BackupSyncProvider
import com.hotelski.waterme.data.local.entity.HistoryAction
import com.hotelski.waterme.data.local.entity.MeasurementUnits
import com.hotelski.waterme.data.local.entity.NotificationPermissionState
import com.hotelski.waterme.data.local.entity.ThemePreference
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
        SettingsUiState(
            isLoading = false,
            profileName = settings.profileName,
            notificationsEnabled = settings.notificationsEnabled,
            notificationPermissionLabel = settings.notificationPermissionState.toPermissionLabel(),
            defaultReminderHour = settings.defaultReminderHour,
            defaultReminderMinute = settings.defaultReminderMinute,
            themePreference = settings.themePreference.toUiPreference(),
            darkModeEnabled = settings.themePreference == ThemePreference.DARK,
            measurementUnits = settings.measurementUnits.toUiUnits(),
            backupSyncEnabled = settings.backupSyncEnabled,
            backupProviderLabel = settings.backupSyncProvider.toProviderLabel(),
            lastBackupLabel = settings.lastBackupAt.toTimestampLabel("Never backed up"),
            lastRestoreLabel = settings.lastRestoreAt.toTimestampLabel("Never restored"),
            localOnlyMode = settings.localOnlyMode,
            analyticsEnabled = settings.analyticsEnabled,
            diagnosticsEnabled = settings.diagnosticsEnabled,
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
            SettingsEvent.RequestNotificationPermissionClicked -> emitEffect(SettingsEffect.RequestNotificationPermission)
            is SettingsEvent.ProfileNameChanged -> updateProfileName(event.value)
            is SettingsEvent.NotificationsChanged -> updateNotifications(event.enabled)
            is SettingsEvent.DefaultReminderTimeChanged -> updateDefaultReminderTime(event.hour, event.minute)
            is SettingsEvent.ThemePreferenceChanged -> updateTheme(event.preference)
            is SettingsEvent.DarkModeChanged -> updateDarkMode(event.enabled)
            is SettingsEvent.MeasurementUnitsChanged -> updateUnits(event.units)
            is SettingsEvent.BackupSyncChanged -> updateBackup(event.enabled)
            SettingsEvent.BackupNowClicked -> markBackupCompleted()
            SettingsEvent.RestoreBackupClicked -> markRestoreCompleted()
            is SettingsEvent.LocalOnlyModeChanged -> updateLocalOnlyMode(event.enabled)
            is SettingsEvent.AnalyticsChanged -> updateAnalytics(event.enabled)
            is SettingsEvent.DiagnosticsChanged -> updateDiagnostics(event.enabled)
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

    private fun updateProfileName(value: String) {
        viewModelScope.launch {
            val trimmedName = value.take(MAX_PROFILE_NAME_LENGTH)
            runCatching { settingsDataStore.updateProfileName(trimmedName) }
                .onSuccess { actionState.value = SettingsActionState(successMessage = "Profile updated.") }
                .onFailure { actionState.value = SettingsActionState(errorMessage = it.toUserMessage()) }
        }
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

    private fun updateDefaultReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            runCatching { settingsDataStore.updateDefaultReminderTime(hour, minute) }
                .onSuccess { actionState.value = SettingsActionState(successMessage = "Default reminder time updated.") }
                .onFailure { actionState.value = SettingsActionState(errorMessage = it.toUserMessage()) }
        }
    }

    private fun updateTheme(preference: SettingsThemePreference) {
        viewModelScope.launch {
            runCatching { settingsDataStore.updateThemePreference(preference.toDataStorePreference()) }
                .onSuccess { actionState.value = SettingsActionState(successMessage = "Appearance updated.") }
                .onFailure { actionState.value = SettingsActionState(errorMessage = it.toUserMessage()) }
        }
    }

    private fun updateDarkMode(enabled: Boolean) {
        updateTheme(if (enabled) SettingsThemePreference.DARK else SettingsThemePreference.SYSTEM)
    }

    private fun updateUnits(units: SettingsMeasurementUnits) {
        viewModelScope.launch {
            runCatching { settingsDataStore.updateMeasurementUnits(units.toDataStoreUnits()) }
                .onSuccess { actionState.value = SettingsActionState(successMessage = "Units updated.") }
                .onFailure { actionState.value = SettingsActionState(errorMessage = it.toUserMessage()) }
        }
    }

    private fun updateBackup(enabled: Boolean) {
        viewModelScope.launch {
            runCatching {
                settingsDataStore.updateBackupSync(
                    enabled = enabled,
                    provider = if (enabled) BackupSyncProvider.GOOGLE_DRIVE else BackupSyncProvider.NONE,
                )
            }
                .onSuccess { actionState.value = SettingsActionState(successMessage = "Backup preference updated.") }
                .onFailure { actionState.value = SettingsActionState(errorMessage = it.toUserMessage()) }
        }
    }

    private fun markBackupCompleted() {
        viewModelScope.launch {
            runCatching { settingsDataStore.markBackupCompleted(System.currentTimeMillis()) }
                .onSuccess { actionState.value = SettingsActionState(successMessage = "Backup snapshot saved locally.") }
                .onFailure { actionState.value = SettingsActionState(errorMessage = it.toUserMessage()) }
        }
    }

    private fun markRestoreCompleted() {
        viewModelScope.launch {
            runCatching { settingsDataStore.markRestoreCompleted(System.currentTimeMillis()) }
                .onSuccess { actionState.value = SettingsActionState(successMessage = "Restore check completed.") }
                .onFailure { actionState.value = SettingsActionState(errorMessage = it.toUserMessage()) }
        }
    }

    private fun updateLocalOnlyMode(enabled: Boolean) {
        viewModelScope.launch {
            runCatching { settingsDataStore.updatePrivacySettings(localOnlyMode = enabled) }
                .onSuccess { actionState.value = SettingsActionState(successMessage = "Privacy preference updated.") }
                .onFailure { actionState.value = SettingsActionState(errorMessage = it.toUserMessage()) }
        }
    }

    private fun updateAnalytics(enabled: Boolean) {
        viewModelScope.launch {
            runCatching { settingsDataStore.updatePrivacySettings(analyticsEnabled = enabled) }
                .onSuccess { actionState.value = SettingsActionState(successMessage = "Analytics preference updated.") }
                .onFailure { actionState.value = SettingsActionState(errorMessage = it.toUserMessage()) }
        }
    }

    private fun updateDiagnostics(enabled: Boolean) {
        viewModelScope.launch {
            runCatching { settingsDataStore.updatePrivacySettings(diagnosticsEnabled = enabled) }
                .onSuccess { actionState.value = SettingsActionState(successMessage = "Diagnostics preference updated.") }
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

    private fun ThemePreference.toUiPreference(): SettingsThemePreference =
        when (this) {
            ThemePreference.SYSTEM -> SettingsThemePreference.SYSTEM
            ThemePreference.LIGHT -> SettingsThemePreference.LIGHT
            ThemePreference.DARK -> SettingsThemePreference.DARK
        }

    private fun SettingsThemePreference.toDataStorePreference(): ThemePreference =
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

    private fun SettingsMeasurementUnits.toDataStoreUnits(): MeasurementUnits =
        when (this) {
            SettingsMeasurementUnits.METRIC -> MeasurementUnits.METRIC
            SettingsMeasurementUnits.IMPERIAL -> MeasurementUnits.IMPERIAL
        }

    private fun BackupSyncProvider.toProviderLabel(): String =
        when (this) {
            BackupSyncProvider.NONE -> "Local only"
            BackupSyncProvider.GOOGLE_DRIVE -> "Google Drive"
            BackupSyncProvider.CLOUD_SYNC -> "Cloud sync"
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
        const val MAX_PROFILE_NAME_LENGTH = 40
        val timestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")
    }
}
