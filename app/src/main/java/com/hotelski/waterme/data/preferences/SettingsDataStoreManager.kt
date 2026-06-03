package com.hotelski.waterme.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hotelski.waterme.data.local.entity.BackupSyncProvider
import com.hotelski.waterme.data.local.entity.MeasurementUnits
import com.hotelski.waterme.data.local.entity.NotificationPermissionState
import com.hotelski.waterme.data.local.entity.ThemePreference
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.waterMeSettingsDataStore by preferencesDataStore(name = "waterme_settings")

data class SettingsPreferences(
    val profileName: String = "Plant keeper",
    val selectedCharacterId: String = DEFAULT_CHARACTER_ID,
    val notificationsEnabled: Boolean = true,
    val notificationPermissionState: NotificationPermissionState = NotificationPermissionState.NOT_REQUESTED,
    val defaultReminderHour: Int = 9,
    val defaultReminderMinute: Int = 0,
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val textColorPreference: TextColorPreference = TextColorPreference.FOREST,
    val measurementUnits: MeasurementUnits = MeasurementUnits.METRIC,
    val backupSyncEnabled: Boolean = false,
    val backupSyncProvider: BackupSyncProvider = BackupSyncProvider.NONE,
    val lastBackupAt: Long? = null,
    val lastRestoreAt: Long? = null,
    val localOnlyMode: Boolean = true,
    val analyticsEnabled: Boolean = false,
    val diagnosticsEnabled: Boolean = false,
    val appOpenDayStreak: Int = 0,
    val lastAppOpenDayEpoch: Long? = null,
)

enum class TextColorPreference {
    FOREST,
    MINT,
    BLUE,
    SKY,
    CLAY,
    AMBER,
    ROSE,
    LAVENDER,
    SLATE,
    HIGH_CONTRAST,
}

object WaterMePreferenceKeys {
    val PROFILE_NAME = stringPreferencesKey("profile_name")
    val SELECTED_CHARACTER_ID = stringPreferencesKey("selected_character_id")
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    val NOTIFICATION_PERMISSION_STATE = stringPreferencesKey("notification_permission_state")
    val DEFAULT_REMINDER_HOUR = intPreferencesKey("default_reminder_hour")
    val DEFAULT_REMINDER_MINUTE = intPreferencesKey("default_reminder_minute")
    val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
    val TEXT_COLOR_PREFERENCE = stringPreferencesKey("text_color_preference")
    val MEASUREMENT_UNITS = stringPreferencesKey("measurement_units")
    val BACKUP_SYNC_ENABLED = booleanPreferencesKey("backup_sync_enabled")
    val BACKUP_SYNC_PROVIDER = stringPreferencesKey("backup_sync_provider")
    val LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
    val LAST_RESTORE_AT = longPreferencesKey("last_restore_at")
    val LOCAL_ONLY_MODE = booleanPreferencesKey("local_only_mode")
    val ANALYTICS_ENABLED = booleanPreferencesKey("analytics_enabled")
    val DIAGNOSTICS_ENABLED = booleanPreferencesKey("diagnostics_enabled")
    val USER_DATA_CLEARED = booleanPreferencesKey("user_data_cleared")
    val APP_OPEN_DAY_STREAK = intPreferencesKey("app_open_day_streak")
    val LAST_APP_OPEN_DAY_EPOCH = longPreferencesKey("last_app_open_day_epoch")
}

class SettingsDataStoreManager(
    context: Context,
) {
    private val dataStore: DataStore<Preferences> = context.applicationContext.waterMeSettingsDataStore

    val settings: Flow<SettingsPreferences> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences -> preferences.toSettingsPreferences() }

    suspend fun updateProfileName(name: String) {
        dataStore.edit { preferences ->
            preferences[WaterMePreferenceKeys.PROFILE_NAME] = name.trim().ifBlank { "Plant keeper" }
        }
    }

    suspend fun updateSelectedCharacterId(characterId: String) {
        dataStore.edit { preferences ->
            preferences[WaterMePreferenceKeys.SELECTED_CHARACTER_ID] = characterId
        }
    }

    suspend fun updateNotificationSettings(
        enabled: Boolean,
        permissionState: NotificationPermissionState,
    ) {
        dataStore.edit { preferences ->
            preferences[WaterMePreferenceKeys.NOTIFICATIONS_ENABLED] = enabled
            preferences[WaterMePreferenceKeys.NOTIFICATION_PERMISSION_STATE] = permissionState.name
        }
    }

    suspend fun updateDefaultReminderTime(hour: Int, minute: Int) {
        dataStore.edit { preferences ->
            preferences[WaterMePreferenceKeys.DEFAULT_REMINDER_HOUR] = hour.coerceIn(0, 23)
            preferences[WaterMePreferenceKeys.DEFAULT_REMINDER_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun updateThemePreference(themePreference: ThemePreference) {
        dataStore.edit { preferences ->
            preferences[WaterMePreferenceKeys.THEME_PREFERENCE] = themePreference.name
        }
    }

    suspend fun updateTextColorPreference(textColorPreference: TextColorPreference) {
        dataStore.edit { preferences ->
            preferences[WaterMePreferenceKeys.TEXT_COLOR_PREFERENCE] = textColorPreference.name
        }
    }

    suspend fun resetColorScheme() {
        dataStore.edit { preferences ->
            preferences[WaterMePreferenceKeys.THEME_PREFERENCE] = ThemePreference.SYSTEM.name
            preferences[WaterMePreferenceKeys.TEXT_COLOR_PREFERENCE] = TextColorPreference.FOREST.name
        }
    }

    suspend fun updateMeasurementUnits(measurementUnits: MeasurementUnits) {
        dataStore.edit { preferences ->
            preferences[WaterMePreferenceKeys.MEASUREMENT_UNITS] = measurementUnits.name
        }
    }

    suspend fun updateBackupSync(
        enabled: Boolean,
        provider: BackupSyncProvider,
    ) {
        dataStore.edit { preferences ->
            preferences[WaterMePreferenceKeys.BACKUP_SYNC_ENABLED] = enabled
            preferences[WaterMePreferenceKeys.BACKUP_SYNC_PROVIDER] = provider.name
        }
    }

    suspend fun markBackupCompleted(completedAt: Long) {
        dataStore.edit { preferences ->
            preferences[WaterMePreferenceKeys.LAST_BACKUP_AT] = completedAt
        }
    }

    suspend fun markRestoreCompleted(completedAt: Long) {
        dataStore.edit { preferences ->
            preferences[WaterMePreferenceKeys.LAST_RESTORE_AT] = completedAt
        }
    }

    suspend fun updatePrivacySettings(
        localOnlyMode: Boolean? = null,
        analyticsEnabled: Boolean? = null,
        diagnosticsEnabled: Boolean? = null,
    ) {
        dataStore.edit { preferences ->
            if (localOnlyMode != null) {
                preferences[WaterMePreferenceKeys.LOCAL_ONLY_MODE] = localOnlyMode
            }
            if (analyticsEnabled != null) {
                preferences[WaterMePreferenceKeys.ANALYTICS_ENABLED] = analyticsEnabled
            }
            if (diagnosticsEnabled != null) {
                preferences[WaterMePreferenceKeys.DIAGNOSTICS_ENABLED] = diagnosticsEnabled
            }
        }
    }

    suspend fun recordAppOpen(todayEpochDay: Long = LocalDate.now().toEpochDay()) {
        dataStore.edit { preferences ->
            val lastOpenDay = preferences[WaterMePreferenceKeys.LAST_APP_OPEN_DAY_EPOCH]
            if (lastOpenDay == todayEpochDay) return@edit

            val currentStreak = preferences[WaterMePreferenceKeys.APP_OPEN_DAY_STREAK] ?: 0
            preferences[WaterMePreferenceKeys.LAST_APP_OPEN_DAY_EPOCH] = todayEpochDay
            preferences[WaterMePreferenceKeys.APP_OPEN_DAY_STREAK] = if (lastOpenDay == todayEpochDay - 1) {
                currentStreak + 1
            } else {
                1
            }
        }
    }

    suspend fun shouldSkipSeedData(): Boolean =
        dataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }
            .map { preferences -> preferences[WaterMePreferenceKeys.USER_DATA_CLEARED] ?: false }
            .first()

    suspend fun clearAfterDeleteAllData() {
        dataStore.edit { preferences ->
            preferences.clear()
            preferences[WaterMePreferenceKeys.USER_DATA_CLEARED] = true
        }
    }
}

private fun Preferences.toSettingsPreferences(): SettingsPreferences =
    SettingsPreferences(
        profileName = this[WaterMePreferenceKeys.PROFILE_NAME] ?: "Plant keeper",
        selectedCharacterId = this[WaterMePreferenceKeys.SELECTED_CHARACTER_ID] ?: DEFAULT_CHARACTER_ID,
        notificationsEnabled = this[WaterMePreferenceKeys.NOTIFICATIONS_ENABLED] ?: true,
        notificationPermissionState = enumPreference(
            this[WaterMePreferenceKeys.NOTIFICATION_PERMISSION_STATE],
            NotificationPermissionState.NOT_REQUESTED,
        ),
        defaultReminderHour = this[WaterMePreferenceKeys.DEFAULT_REMINDER_HOUR] ?: 9,
        defaultReminderMinute = this[WaterMePreferenceKeys.DEFAULT_REMINDER_MINUTE] ?: 0,
        themePreference = enumPreference(this[WaterMePreferenceKeys.THEME_PREFERENCE], ThemePreference.SYSTEM),
        textColorPreference = enumPreference(
            this[WaterMePreferenceKeys.TEXT_COLOR_PREFERENCE],
            TextColorPreference.FOREST,
        ),
        measurementUnits = enumPreference(this[WaterMePreferenceKeys.MEASUREMENT_UNITS], MeasurementUnits.METRIC),
        backupSyncEnabled = this[WaterMePreferenceKeys.BACKUP_SYNC_ENABLED] ?: false,
        backupSyncProvider = enumPreference(this[WaterMePreferenceKeys.BACKUP_SYNC_PROVIDER], BackupSyncProvider.NONE),
        lastBackupAt = this[WaterMePreferenceKeys.LAST_BACKUP_AT],
        lastRestoreAt = this[WaterMePreferenceKeys.LAST_RESTORE_AT],
        localOnlyMode = this[WaterMePreferenceKeys.LOCAL_ONLY_MODE] ?: true,
        analyticsEnabled = this[WaterMePreferenceKeys.ANALYTICS_ENABLED] ?: false,
        diagnosticsEnabled = this[WaterMePreferenceKeys.DIAGNOSTICS_ENABLED] ?: false,
        appOpenDayStreak = this[WaterMePreferenceKeys.APP_OPEN_DAY_STREAK] ?: 0,
        lastAppOpenDayEpoch = this[WaterMePreferenceKeys.LAST_APP_OPEN_DAY_EPOCH],
    )

private const val DEFAULT_CHARACTER_ID = "SPROUT"

private inline fun <reified T : Enum<T>> enumPreference(
    value: String?,
    defaultValue: T,
): T =
    value
        ?.let { runCatching { enumValueOf<T>(it) }.getOrNull() }
        ?: defaultValue
